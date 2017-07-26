package io.flow.lib

import io.flow.localized.items.cache.v0.models.{LocalizedItemIncludedLevyKey, LocalizedItemPrices, LocalizedPricing}

/**
  * Represents the local pricing needed for a given sku
  * @param data The local pricing data for the sku
  */
case class FlowSkuPrice(data: LocalizedPricing) {

  val salePrice: LocalizedItemPrices = data.price

  val msrpPrice: Option[LocalizedItemPrices] = data.attributes.get("msrp")

  val basePrice: Option[LocalizedItemPrices] = data.attributes.get("base")

  val shippingSurcharge: Option[LocalizedItemPrices] = data.attributes.get("shipping_surcharge")

  val includes: Option[String] = data.includes.map {
    case LocalizedItemIncludedLevyKey.Duty => "Duties"
    case LocalizedItemIncludedLevyKey.VatAndDuty => "VAT and Duties"
    case LocalizedItemIncludedLevyKey.Vat => "VAT"
    case LocalizedItemIncludedLevyKey.UNDEFINED(other) => other
  }

}
