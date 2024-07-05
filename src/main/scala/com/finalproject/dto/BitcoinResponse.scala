package com.finalproject.dto

import java.time.ZonedDateTime
import io.circe.*
import io.circe.generic.semiauto.*


case class Time(updatedISO: ZonedDateTime)

case class Quote(code: String, rate: String, description: String, rate_float: BigDecimal)

case class BPI(USD: Quote, GBP: Quote, EUR: Quote)

case class BitcoinResponse(time: Time, disclaimer: String, chartName: String, bpi: BPI)
