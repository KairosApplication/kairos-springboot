package com.kairos.kairosapipostgres.utils;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

class CpfValidatorTest {

    @ParameterizedTest
    @ValueSource(strings = {"52998224725", "529.982.247-25"})
    void shouldAcceptValidCpf(String cpf) {
        assertThat(CpfValidator.isValid(cpf)).isTrue();
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"", "123", "52998224724", "11111111111", "abcdefghijk"})
    void shouldRejectInvalidCpf(String cpf) {
        assertThat(CpfValidator.isValid(cpf)).isFalse();
    }
}
