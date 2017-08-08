package io.flow.localization

import io.flow.catalog.v0.models.LocalizedItemPrice
import io.flow.common.v0.models.PriceWithBase
import io.flow.item.v0.models.LocalItemPricing

/**
  * Represents the local pricing needed for a given sku
  **/
case class FlowSkuPrice(
                         salePrice: LocalizedItemPrice,
                         msrpPrice: Option[PriceWithBase],
                         basePrice: Option[PriceWithBase],
                         shippingSurcharge: Option[PriceWithBase]
                       )

object FlowSkuPrice {

  def apply(pricing: LocalItemPricing): FlowSkuPrice = {
    FlowSkuPrice(
      salePrice = pricing.price,
      msrpPrice = pricing.attributes.get("msrp_price"),
      basePrice = pricing.attributes.get("base_price"),
      shippingSurcharge = pricing.attributes.get("shipping_surcharge")
    )
  }

}
