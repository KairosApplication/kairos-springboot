package com.kairos.kairosapipostgres.utils;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CpfFormatterTest {

    @Test
    void shouldRemoveCpfMask() {
        assertThat(CpfFormatter.removeFormatMask("529.982.247-25"))
                .isEqualTo("52998224725");
    }

    @Test
    void shouldAddCpfMask() {
        assertThat(CpfFormatter.addFormatMask("52998224725"))
                .isEqualTo("529.982.247-25");
    }

    @Test
    void shouldReturnNullForNullOrWrongLengthCpf() {
        assertThat(CpfFormatter.removeFormatMask(null)).isNull();
        assertThat(CpfFormatter.addFormatMask(null)).isNull();
        assertThat(CpfFormatter.addFormatMask("123")).isNull();
    }
}
