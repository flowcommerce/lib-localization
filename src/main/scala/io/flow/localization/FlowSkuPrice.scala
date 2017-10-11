package io.flow.localization

import scala.reflect.ClassTag

/**
  * Represents the local pricing needed for a given sku
  **/
case class FlowSkuPrice(
                         currency: String,
                         salePrice: BigDecimal,
                         msrpPrice: Option[BigDecimal],
                         basePrice: Option[BigDecimal],
                         shippingSurcharge: Option[BigDecimal],
                         includesLabel: Option[String]
                       )

object FlowSkuPrice {

  val CurrencyKey = "c"
  val SalePriceKey = "a"
  val MsrpPriceKey = "m"
  val BasePriceKey = "s"
  val ShippingSurchargeKey = "s"
  val IncludesLabelKey = "i"

  def apply(attributes: Map[String, Any]): Option[FlowSkuPrice] = {
    for {
      currency <- get[String](attributes, CurrencyKey)
      amount <- get[Number](attributes, SalePriceKey).map(_.doubleValue())
    } yield FlowSkuPrice(
      currency = currency,
      salePrice = amount,
      msrpPrice = get[Number](attributes, MsrpPriceKey).map(_.doubleValue()),
      basePrice = get[Number](attributes, BasePriceKey).map(_.doubleValue()),
      shippingSurcharge = get[Number](attributes, ShippingSurchargeKey).map(_.doubleValue()),
      includesLabel = get[String](attributes, IncludesLabelKey)
    )
  }

  private def get[T: ClassTag](attributes: Map[String, Any], key: String): Option[T] =
    attributes.get(key).collect { case v: T => v }

}
