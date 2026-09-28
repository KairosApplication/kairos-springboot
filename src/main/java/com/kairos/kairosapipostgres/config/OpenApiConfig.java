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
    private static final String SESSION = "sessionCookie";

    @Bean
    public OpenAPI kairosOpenApi() {
        return new OpenAPI()
                .info(new Info().title("Kairos API")
                        .version("v1")
                        .description("API REST do Kairos. Autenticação por sessão JSESSIONID. "
                                + "Antes de POST/PATCH/DELETE, obtenha o token em GET /api/v1/auth/login "
                                + "e envie o valor no header indicado pela resposta. Após o login, renove o token CSRF."))
                .components(new Components().addSchemas("ApiErrorResponse", new ObjectSchema()
                                .addProperty("status", new Schema<>().type("integer"))
                                .addProperty("error", new StringSchema())
                                .addProperty("message", new StringSchema())
                                .addProperty("validationErrors", new ObjectSchema()))
                        .addSecuritySchemes(SESSION, new SecurityScheme().type(SecurityScheme.Type.APIKEY)
                                .in(SecurityScheme.In.COOKIE).name("JSESSIONID")
                                .description("Cookie de sessão criado pelo POST de login.")));
    }

    @Bean
    public OpenApiCustomizer documentOperations() {
        return api -> {
            document(api, "/api/v1/auth/login", PathItem.HttpMethod.GET, "Autenticação",
                    "Obter token CSRF", "Inicia uma sessão anônima e devolve o nome do header e o token CSRF.",
                    200, false, false);
            documentLogin(api);
            document(api, "/api/v1/auth/me", PathItem.HttpMethod.GET, "Autenticação",
                    "Consultar sessão", "Retorna e-mail e roles da sessão atual.", 200, true, false);
            document(api, "/api/v1/auth/logout", PathItem.HttpMethod.POST, "Autenticação",
                    "Encerrar sessão", "Invalida a sessão e remove o cookie JSESSIONID.", 204, true, true);

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
        };
    }

    private void resource(OpenAPI api, String name, String tag, boolean publicRegistration) {
        String base = "/api/v1/" + name;
        String permission = switch (name) {
            case "employees" -> "Exige role MANAGER.";
            case "products" -> "Leitura exige role CUSTOMER; escrita está bloqueada pela segurança atual.";
            default -> "Exige sessão autenticada.";
        };
        document(api, base + "/registration", PathItem.HttpMethod.POST, tag,
                "Cadastrar " + tag.toLowerCase(), permission, 201, !publicRegistration, true);
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
        if (changesState) {
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
            responses.addApiResponse("401", new ApiResponse().description("Sessão ausente ou expirada"));
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
                .description("Envia e-mail e senha como formulário. O Spring Security cria a sessão e "
                        + "renova o token CSRF. Use o cookie retornado nas próximas chamadas.")
                .addParametersItem(new Parameter().in("header").name("X-CSRF-TOKEN")
                        .required(true).schema(new StringSchema())
                        .description("Token obtido em GET /api/v1/auth/login."))
                .requestBody(new RequestBody().required(true).content(new Content().addMediaType(
                        "application/x-www-form-urlencoded",
                        new io.swagger.v3.oas.models.media.MediaType().schema(form))))
                .responses(new ApiResponses()
                        .addApiResponse("204", new ApiResponse().description("Sessão criada"))
                        .addApiResponse("401", new ApiResponse().description("Credenciais inválidas"))
                        .addApiResponse("403", new ApiResponse().description("Token CSRF inválido")));
        api.getPaths().get("/api/v1/auth/login").post(login);
    }
}
