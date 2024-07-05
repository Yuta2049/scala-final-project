package com.finalproject.dto

import java.time.LocalDateTime

case class QuoteDb(currencyCode: String, quoteDatetime: LocalDateTime, rate: BigDecimal)
