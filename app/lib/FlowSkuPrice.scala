package io.flow.lib

import io.flow.localized.items.cache.v0.models.{LocalizedItemCacheIncludedLevyKey, LocalizedItemCachePrices, LocalizedItemCachePricing}

/**
  * Represents the local pricing needed for a given sku
  **/
case class FlowSkuPrice(
                         salePrice: LocalizedItemCachePrices,
                         msrpPrice: Option[LocalizedItemCachePrices],
                         basePrice: Option[LocalizedItemCachePrices],
                         shippingSurcharge: Option[LocalizedItemCachePrices],
                         includes: Option[String]
                       )

object FlowSkuPrice {

  def apply(pricing: LocalizedItemCachePricing): FlowSkuPrice = {
    FlowSkuPrice(
      salePrice = pricing.price,
      msrpPrice = pricing.attributes.get("msrp"),
      basePrice = pricing.attributes.get("base"),
      shippingSurcharge = pricing.attributes.get("shipping_surcharge"),
      includes = pricing.includes.map(toIncludedLabel)
    )
  }

  private def toIncludedLabel(key: LocalizedItemCacheIncludedLevyKey): String = key match {
    case LocalizedItemCacheIncludedLevyKey.Duty => "Duties"
    case LocalizedItemCacheIncludedLevyKey.VatAndDuty => "VAT and Duties"
    case LocalizedItemCacheIncludedLevyKey.Vat => "VAT"
    case LocalizedItemCacheIncludedLevyKey.UNDEFINED(other) => other
  }

}
