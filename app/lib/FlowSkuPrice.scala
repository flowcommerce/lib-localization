package io.flow.lib

import io.flow.common.v0.models.PriceWithBase
import io.flow.localized.items.cache.v0.models.LocalizedPricing

/**
  * Represents the local pricing needed for a given sku
  * @param data The local pricing data for the sku
  */
case class FlowSkuPrice(data: LocalizedPricing) {

  val salePrice: PriceWithBase = findPrice("price").getOrElse {
    sys.error(s"data do not contain a price key 'price': $data")
  }

  val msrpPrice: Option[PriceWithBase] = findPrice("msrp")

  val basePrice: Option[PriceWithBase] = findPrice("base")

  val shippingSurcharge: Option[PriceWithBase] = findPrice("shipping_surcharge")

  /**
    * e.g. "VAT" or "VAT and Duty"
    */
  val includes: Option[String] = data.includes

  private[this] def findPrice(key: String): Option[PriceWithBase] = {
    data.prices.find(_.key == key).map(_.price)
  }

}
