package com.kairos.kairosapipostgres.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.media.StringSchema;
import io.swagger.v3.oas.models.media.ObjectSchema;
import io.swagger.v3.oas.models.parameters.Parameter;
import io.swagger.v3.oas.models.parameters.RequestBody;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.responses.ApiResponses;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenApiConfig {
    private static final String SESSION = "bearerAuth";

    @Bean
    public OpenAPI kairosOpenApi() {
        return new OpenAPI()
                .info(new Info().title("Kairos API")
                        .version("v1")
                        .description("API REST do Kairos. JWT Bearer de 15 minutos e refresh em cookie HttpOnly, "
                                + "com sessão absoluta de 7 dias. Login, cadastro público, refresh e logout exigem "
                                + "CSRF obtido em GET /api/v1/auth/login. Após login/cadastro, renove o CSRF. "
                                + "Logout e reuso invalidam a sessão e os JWTs emitidos. Cookies permitem renovação automática na API."))
                .components(new Components().addSchemas("ApiErrorResponse", new ObjectSchema()
                                .addProperty("status", new Schema<>().type("integer"))
                                .addProperty("error", new StringSchema())
                                .addProperty("message", new StringSchema())
                                .addProperty("validationErrors", new ObjectSchema()))
                        .addSecuritySchemes(SESSION, new SecurityScheme().type(SecurityScheme.Type.HTTP)
                                .scheme("bearer").bearerFormat("JWT")
                                .description("Access token retornado pelo login ou refresh.")));
    }

    @Bean
    public OpenApiCustomizer documentOperations() {
        return api -> {
            document(api, "/api/v1/auth/login", PathItem.HttpMethod.GET, "Autenticação",
                    "Obter token CSRF", "Cria cookie CSRF e devolve o nome do header e o token CSRF.",
                    200, false, false);
            documentLogin(api);
            document(api, "/api/v1/auth/me", PathItem.HttpMethod.GET, "Autenticação",
                    "Consultar identidade", "Valida o JWT e a sessão no Redis e retorna e-mail e roles. "
                            + "Renova automaticamente com refresh cookie válido; retorna 401 se a sessão for inválida.", 200, true, false);
            document(api, "/api/v1/auth/logout", PathItem.HttpMethod.POST, "Autenticação",
                    "Encerrar sessão", "Revoga a sessão provada pelo cookie ou JWT e remove os cookies. "
                            + "Idempotente. JWTs emitidos são rejeitados na próxima requisição.", 204, false, true);
            document(api, "/api/v1/auth/refresh", PathItem.HttpMethod.POST, "Autenticação",
                    "Renovar tokens", "Exige refresh cookie e CSRF. Rotaciona o refresh atomicamente no Redis. "
                            + "Reuso revoga a família; serialize chamadas no cliente.", 200, false, true);
            for (String path : List.of("/api/v1/auth/login", "/api/v1/auth/refresh", "/api/v1/auth/logout")) {
                api.getPaths().get(path).getPost().getResponses()
                        .addApiResponse("401", error("Autenticação inválida"))
                        .addApiResponse("503", error("Infraestrutura indisponível"));
            }

            resource(api, "users", "Usuários", true);
            resource(api, "employees", "Funcionários", false);
            resource(api, "customers", "Clientes", false);
            resource(api, "categories", "Categorias", false);
            resource(api, "sectors", "Setores", false);
            resource(api, "products", "Produtos", false);

            document(api, "/api/v1/products/list/{brand}", PathItem.HttpMethod.GET, "Produtos",
                    "Listar por marca", "Retorna produtos da marca informada. Exige role CUSTOMER.",
                    200, true, false);
            document(api, "/api/v1/products/find/price", PathItem.HttpMethod.GET, "Produtos",
                    "Filtrar por preço", "Retorna produtos com preço até maxPrice, inclusive. "
                            + "Exige role CUSTOMER.", 200, true, false);
            document(api, "/api/v1/products/find/name", PathItem.HttpMethod.GET, "Produtos",
                    "Buscar por nome", "Retorna o produto com o nome informado. Exige role CUSTOMER.",
                    200, true, false);
            document(api, "/api/v1/sectors/find/name", PathItem.HttpMethod.GET, "Setores",
                    "Buscar por nome", "Retorna o setor com o nome informado.", 200, true, false);
            document(api, "/api/v1/sectors/find/type", PathItem.HttpMethod.GET, "Setores",
                    "Filtrar por tipo", "Retorna os setores do tipo informado.", 200, true, false);
            document(api, "/api/v1/customers/{id}/recommendations", PathItem.HttpMethod.GET, "Clientes",
                    "Recomendar produtos", "Retorna até limit produtos para o cliente. Somente o próprio cliente "
                            + "ou um gerente pode consultar. limit aceita valores de 1 a 100 e usa 10 por padrão.",
                    200, true, false);
            document(api, "/api/v1/purchases/{id}/total", PathItem.HttpMethod.GET, "Compras",
                    "Calcular total", "Retorna o valor total da compra. Exige role MANAGER.",
                    200, true, false);
            document(api, "/api/v1/store-map", PathItem.HttpMethod.GET, "Mapa da loja",
                    "Consultar mapa da empresa", "Retorna setores, corredores, gôndolas, produtos e vínculos "
                            + "da empresa autenticada. Exige MANAGER vinculado a funcionário com empresa.",
                    200, true, false);
            document(api, "/api/v1/restockings/week", PathItem.HttpMethod.GET, "Reposições",
                    "Histórico semanal de reposições", "Semana atual, de segunda a segunda em America/Sao_Paulo. "
                            + "Repositor consulta apenas o próprio histórico; gerente pode informar employeeId da mesma empresa. "
                            + "limit aceita 1 a 500, padrão 100. PostgreSQL recupera cache vazio ou indisponível.",
                    200, true, false);
        };
    }

    private void resource(OpenAPI api, String name, String tag, boolean publicRegistration) {
        String base = "/api/v1/" + name;
        String permission = switch (name) {
            case "employees" -> "Exige role MANAGER.";
            case "products" -> "Leitura exige CUSTOMER, EMPLOYEE ou MANAGER; escrita exige MANAGER.";
            default -> "Exige JWT e as permissões configuradas para o recurso.";
        };
        document(api, base + "/registration", PathItem.HttpMethod.POST, tag,
                "Cadastrar " + tag.toLowerCase(), publicRegistration
                        ? "Cadastro público exige companyId de uma empresa existente, cria CUSTOMER "
                        + "e retorna accessToken, tokenType, expiresIn e refresh cookie ao visitante. "
                        + "Um JWT já autenticado mantém seu usuário atual. Após o cadastro, renove o token CSRF."
                        : permission, 201, !publicRegistration, true);
        document(api, base + "/list", PathItem.HttpMethod.GET, tag,
                "Listar " + tag.toLowerCase(), permission, 200, true, false);
        document(api, base + "/find/{id}", PathItem.HttpMethod.GET, tag,
                "Buscar por ID", permission, 200, true, false);
        if (!"customers".equals(name)) {
            document(api, base + "/update/{id}", PathItem.HttpMethod.PATCH, tag,
                    "Atualizar por ID", permission, 200, true, true);
        }
        document(api, base + "/delete/{id}", PathItem.HttpMethod.DELETE, tag,
                "Excluir por ID", permission, 204, true, true);
    }

    private void document(OpenAPI api, String path, PathItem.HttpMethod method, String tag,
                          String summary, String description, int success, boolean secured,
                          boolean changesState) {
        PathItem item = api.getPaths().get(path);
        if (item == null || item.readOperationsMap().get(method) == null) {
            throw new IllegalStateException("Rota OpenAPI não encontrada: " + method + " " + path);
        }
        Operation operation = item.readOperationsMap().get(method);
        operation.tags(List.of(tag)).summary(summary).description(description);
        if (secured) {
            operation.addSecurityItem(new SecurityRequirement().addList(SESSION));
        }
        if (changesState && !secured) {
            operation.addParametersItem(new Parameter().in("header").name("X-CSRF-TOKEN")
                    .required(true).description("Token obtido em GET /api/v1/auth/login.")
                    .schema(new StringSchema()));
        }
        ApiResponses responses = operation.getResponses();
        ApiResponse generated = responses.remove("200");
        responses.addApiResponse(String.valueOf(success), success == 204
                ? new ApiResponse().description("Sem conteúdo")
                : generated == null ? new ApiResponse().description("Sucesso") : generated);
        if (secured) {
            responses.addApiResponse("401", new ApiResponse().description("JWT ausente ou inválido"));
            responses.addApiResponse("403", new ApiResponse().description(
                    "Acesso negado ou token CSRF inválido em operações de escrita"));
        }
        if (changesState && !secured) {
            responses.addApiResponse("403", new ApiResponse().description("Token CSRF ausente ou inválido"));
        }
        if (method == PathItem.HttpMethod.POST || method == PathItem.HttpMethod.PATCH) {
            responses.addApiResponse("400", error("Dados inválidos"));
            responses.addApiResponse("409", error("Conflito com dados existentes"));
        }
        if (path.contains("{id}")) {
            responses.addApiResponse("404", error("Registro não encontrado"));
        }
        if (path.endsWith("/recommendations") || path.endsWith("/find/price")) {
            responses.addApiResponse("400", error("Parâmetro inválido"));
        }
    }

    private ApiResponse error(String description) {
        return new ApiResponse().description(description).content(new Content().addMediaType(
                "application/json", new io.swagger.v3.oas.models.media.MediaType()
                        .schema(new Schema<>().$ref("#/components/schemas/ApiErrorResponse"))));
    }

    private void documentLogin(OpenAPI api) {
        ObjectSchema form = new ObjectSchema();
        form.addProperty("email", new StringSchema().format("email"));
        form.addProperty("password", new StringSchema().format("password"));
        form.addRequiredItem("email");
        form.addRequiredItem("password");
        Operation login = new Operation().tags(List.of("Autenticação"))
                .summary("Entrar")
                .description("Envia e-mail e senha como formulário. Retorna accessToken, tokenType e expiresIn; "
                        + "refresh exclusivamente no cookie HttpOnly. Obtenha novo CSRF após o login.")
                .addParametersItem(new Parameter().in("header").name("X-CSRF-TOKEN")
                        .required(true).schema(new StringSchema())
                        .description("Token obtido em GET /api/v1/auth/login."))
                .requestBody(new RequestBody().required(true).content(new Content().addMediaType(
                        "application/x-www-form-urlencoded",
                        new io.swagger.v3.oas.models.media.MediaType().schema(form))))
                .responses(new ApiResponses()
                        .addApiResponse("200", new ApiResponse().description("Access token e refresh cookie emitidos")
                                .content(new Content().addMediaType("application/json",
                                        new io.swagger.v3.oas.models.media.MediaType()
                                                .schema(new Schema<>().$ref("#/components/schemas/TokenResponse")))))
                        .addApiResponse("401", new ApiResponse().description("Credenciais inválidas"))
                        .addApiResponse("403", new ApiResponse().description("Token CSRF inválido")));
        api.getPaths().get("/api/v1/auth/login").post(login);
    }
}
