package io.flow.localization.transit.utils

/**
  * Key used for looking up a transit window in the transit window cache
  * @param origin country in the ISO 3166-3 format that the package is being shipped from
  * @param destination country in the ISO 3166-3 format that the package is being delivered to
  */
case class TransitWindowKey(origin: String, destination: String)