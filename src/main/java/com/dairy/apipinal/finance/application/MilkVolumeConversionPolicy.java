package com.dairy.apipinal.finance.application;

import java.math.BigDecimal;

public interface MilkVolumeConversionPolicy {

    BigDecimal convertKgToLitres(BigDecimal volumeKg);
}