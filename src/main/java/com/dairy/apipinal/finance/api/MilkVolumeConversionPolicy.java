package com.dairy.apipinal.finance.api;

import java.math.BigDecimal;

public interface MilkVolumeConversionPolicy {

    BigDecimal convertKgToLitres(BigDecimal volumeKg);
}
