/**
 * Generated by API Builder - https://www.apibuilder.io
 * Service version: 0.4.99
 * apibuilder 0.14.3 app.apibuilder.io/flow/currency/0.4.99/play_2_x_standalone_json
 */
package io.flow.currency.v0.models {

  /**
   * Represents organization-specific currency conversion adjustments.
   * 
   * @param base The base currency's ISO 4217 3-character code as defined in
   *        https://api.flow.io/reference/currencies
   * @param target The target currency's ISO 4217 3-character code as defined in
   *        https://api.flow.io/reference/currencies
   * @param margin A percent of the base currency value added to the base currency before
   *        conversion.
   */
  final case class OrganizationCurrencySetting(
    id: String,
    base: String,
    target: String,
    margin: BigDecimal
  )

  /**
   * Represents the parts of an organization setting that can be updated.
   * 
   * @param base The base currency's ISO 4217 3-character code as defined in
   *        https://api.flow.io/reference/currencies
   * @param target The target currency's ISO 4217 3-character code as defined in
   *        https://api.flow.io/reference/currencies
   * @param margin A percent of the base currency value added to the base currency before
   *        conversion.
   */
  final case class OrganizationCurrencySettingForm(
    base: String,
    target: String,
    margin: BigDecimal
  )

  final case class OrganizationCurrencySettingVersion(
    id: String,
    timestamp: _root_.org.joda.time.DateTime,
    `type`: io.flow.common.v0.models.ChangeType,
    organizationCurrencySetting: io.flow.currency.v0.models.OrganizationCurrencySetting
  )

  /**
   * Represents an organization-specific currency conversion rate at a point in time.
   * 
   * @param base The base currency's ISO 4217 3-character code as defined in
   *        https://api.flow.io/reference/currencies
   * @param target The target currency's ISO 4217 3-character code as defined in
   *        https://api.flow.io/reference/currencies
   * @param effectiveAt The time at which this rate is effective.
   * @param value The actual conversion rate from the base currency to target currency including
   *        any applicable margins.
   */
  final case class Rate(
    id: String,
    base: String,
    target: String,
    effectiveAt: _root_.org.joda.time.DateTime,
    value: BigDecimal
  )

  /**
   * Represents the parts of an organization rate that can be updated.
   * 
   * @param base The base currency's ISO 4217 3-character code as defined in
   *        https://api.flow.io/reference/currencies
   * @param target The target currency's ISO 4217 3-character code as defined in
   *        https://api.flow.io/reference/currencies
   * @param effectiveAt The time at which this rate is effective.
   */
  final case class RateForm(
    base: String,
    target: String,
    effectiveAt: _root_.org.joda.time.DateTime
  )

  final case class RateVersion(
    id: String,
    timestamp: _root_.org.joda.time.DateTime,
    `type`: io.flow.common.v0.models.ChangeType,
    rate: io.flow.currency.v0.models.Rate
  )

}

package io.flow.currency.v0.models {

  package object json {
    import play.api.libs.json.__
    import play.api.libs.json.JsString
    import play.api.libs.json.Writes
    import play.api.libs.functional.syntax._
    import io.flow.common.v0.models.json._
    import io.flow.currency.v0.models.json._
    import io.flow.error.v0.models.json._

    private[v0] implicit val jsonReadsUUID = __.read[String].map(java.util.UUID.fromString)

    private[v0] implicit val jsonWritesUUID = new Writes[java.util.UUID] {
      def writes(x: java.util.UUID) = JsString(x.toString)
    }

    private[v0] implicit val jsonReadsJodaDateTime = __.read[String].map { str =>
      import org.joda.time.format.ISODateTimeFormat.dateTimeParser
      dateTimeParser.parseDateTime(str)
    }

    private[v0] implicit val jsonWritesJodaDateTime = new Writes[org.joda.time.DateTime] {
      def writes(x: org.joda.time.DateTime) = {
        import org.joda.time.format.ISODateTimeFormat.dateTime
        val str = dateTime.print(x)
        JsString(str)
      }
    }

    private[v0] implicit val jsonReadsJodaLocalDate = __.read[String].map { str =>
      import org.joda.time.format.ISODateTimeFormat.dateParser
      dateParser.parseLocalDate(str)
    }

    private[v0] implicit val jsonWritesJodaLocalDate = new Writes[org.joda.time.LocalDate] {
      def writes(x: org.joda.time.LocalDate) = {
        import org.joda.time.format.ISODateTimeFormat.date
        val str = date.print(x)
        JsString(str)
      }
    }

    implicit def jsonReadsCurrencyOrganizationCurrencySetting: play.api.libs.json.Reads[OrganizationCurrencySetting] = {
      (
        (__ \ "id").read[String] and
        (__ \ "base").read[String] and
        (__ \ "target").read[String] and
        (__ \ "margin").read[BigDecimal]
      )(OrganizationCurrencySetting.apply _)
    }

    def jsObjectOrganizationCurrencySetting(obj: io.flow.currency.v0.models.OrganizationCurrencySetting): play.api.libs.json.JsObject = {
      play.api.libs.json.Json.obj(
        "id" -> play.api.libs.json.JsString(obj.id),
        "base" -> play.api.libs.json.JsString(obj.base),
        "target" -> play.api.libs.json.JsString(obj.target),
        "margin" -> play.api.libs.json.JsNumber(obj.margin)
      )
    }

    implicit def jsonWritesCurrencyOrganizationCurrencySetting: play.api.libs.json.Writes[OrganizationCurrencySetting] = {
      new play.api.libs.json.Writes[io.flow.currency.v0.models.OrganizationCurrencySetting] {
        def writes(obj: io.flow.currency.v0.models.OrganizationCurrencySetting) = {
          jsObjectOrganizationCurrencySetting(obj)
        }
      }
    }

    implicit def jsonReadsCurrencyOrganizationCurrencySettingForm: play.api.libs.json.Reads[OrganizationCurrencySettingForm] = {
      (
        (__ \ "base").read[String] and
        (__ \ "target").read[String] and
        (__ \ "margin").read[BigDecimal]
      )(OrganizationCurrencySettingForm.apply _)
    }

    def jsObjectOrganizationCurrencySettingForm(obj: io.flow.currency.v0.models.OrganizationCurrencySettingForm): play.api.libs.json.JsObject = {
      play.api.libs.json.Json.obj(
        "base" -> play.api.libs.json.JsString(obj.base),
        "target" -> play.api.libs.json.JsString(obj.target),
        "margin" -> play.api.libs.json.JsNumber(obj.margin)
      )
    }

    implicit def jsonWritesCurrencyOrganizationCurrencySettingForm: play.api.libs.json.Writes[OrganizationCurrencySettingForm] = {
      new play.api.libs.json.Writes[io.flow.currency.v0.models.OrganizationCurrencySettingForm] {
        def writes(obj: io.flow.currency.v0.models.OrganizationCurrencySettingForm) = {
          jsObjectOrganizationCurrencySettingForm(obj)
        }
      }
    }

    implicit def jsonReadsCurrencyOrganizationCurrencySettingVersion: play.api.libs.json.Reads[OrganizationCurrencySettingVersion] = {
      (
        (__ \ "id").read[String] and
        (__ \ "timestamp").read[_root_.org.joda.time.DateTime] and
        (__ \ "type").read[io.flow.common.v0.models.ChangeType] and
        (__ \ "organization_currency_setting").read[io.flow.currency.v0.models.OrganizationCurrencySetting]
      )(OrganizationCurrencySettingVersion.apply _)
    }

    def jsObjectOrganizationCurrencySettingVersion(obj: io.flow.currency.v0.models.OrganizationCurrencySettingVersion): play.api.libs.json.JsObject = {
      play.api.libs.json.Json.obj(
        "id" -> play.api.libs.json.JsString(obj.id),
        "timestamp" -> play.api.libs.json.JsString(_root_.org.joda.time.format.ISODateTimeFormat.dateTime.print(obj.timestamp)),
        "type" -> play.api.libs.json.JsString(obj.`type`.toString),
        "organization_currency_setting" -> jsObjectOrganizationCurrencySetting(obj.organizationCurrencySetting)
      )
    }

    implicit def jsonWritesCurrencyOrganizationCurrencySettingVersion: play.api.libs.json.Writes[OrganizationCurrencySettingVersion] = {
      new play.api.libs.json.Writes[io.flow.currency.v0.models.OrganizationCurrencySettingVersion] {
        def writes(obj: io.flow.currency.v0.models.OrganizationCurrencySettingVersion) = {
          jsObjectOrganizationCurrencySettingVersion(obj)
        }
      }
    }

    implicit def jsonReadsCurrencyRate: play.api.libs.json.Reads[Rate] = {
      (
        (__ \ "id").read[String] and
        (__ \ "base").read[String] and
        (__ \ "target").read[String] and
        (__ \ "effective_at").read[_root_.org.joda.time.DateTime] and
        (__ \ "value").read[BigDecimal]
      )(Rate.apply _)
    }

    def jsObjectRate(obj: io.flow.currency.v0.models.Rate): play.api.libs.json.JsObject = {
      play.api.libs.json.Json.obj(
        "id" -> play.api.libs.json.JsString(obj.id),
        "base" -> play.api.libs.json.JsString(obj.base),
        "target" -> play.api.libs.json.JsString(obj.target),
        "effective_at" -> play.api.libs.json.JsString(_root_.org.joda.time.format.ISODateTimeFormat.dateTime.print(obj.effectiveAt)),
        "value" -> play.api.libs.json.JsNumber(obj.value)
      )
    }

    implicit def jsonWritesCurrencyRate: play.api.libs.json.Writes[Rate] = {
      new play.api.libs.json.Writes[io.flow.currency.v0.models.Rate] {
        def writes(obj: io.flow.currency.v0.models.Rate) = {
          jsObjectRate(obj)
        }
      }
    }

    implicit def jsonReadsCurrencyRateForm: play.api.libs.json.Reads[RateForm] = {
      (
        (__ \ "base").read[String] and
        (__ \ "target").read[String] and
        (__ \ "effective_at").read[_root_.org.joda.time.DateTime]
      )(RateForm.apply _)
    }

    def jsObjectRateForm(obj: io.flow.currency.v0.models.RateForm): play.api.libs.json.JsObject = {
      play.api.libs.json.Json.obj(
        "base" -> play.api.libs.json.JsString(obj.base),
        "target" -> play.api.libs.json.JsString(obj.target),
        "effective_at" -> play.api.libs.json.JsString(_root_.org.joda.time.format.ISODateTimeFormat.dateTime.print(obj.effectiveAt))
      )
    }

    implicit def jsonWritesCurrencyRateForm: play.api.libs.json.Writes[RateForm] = {
      new play.api.libs.json.Writes[io.flow.currency.v0.models.RateForm] {
        def writes(obj: io.flow.currency.v0.models.RateForm) = {
          jsObjectRateForm(obj)
        }
      }
    }

    implicit def jsonReadsCurrencyRateVersion: play.api.libs.json.Reads[RateVersion] = {
      (
        (__ \ "id").read[String] and
        (__ \ "timestamp").read[_root_.org.joda.time.DateTime] and
        (__ \ "type").read[io.flow.common.v0.models.ChangeType] and
        (__ \ "rate").read[io.flow.currency.v0.models.Rate]
      )(RateVersion.apply _)
    }

    def jsObjectRateVersion(obj: io.flow.currency.v0.models.RateVersion): play.api.libs.json.JsObject = {
      play.api.libs.json.Json.obj(
        "id" -> play.api.libs.json.JsString(obj.id),
        "timestamp" -> play.api.libs.json.JsString(_root_.org.joda.time.format.ISODateTimeFormat.dateTime.print(obj.timestamp)),
        "type" -> play.api.libs.json.JsString(obj.`type`.toString),
        "rate" -> jsObjectRate(obj.rate)
      )
    }

    implicit def jsonWritesCurrencyRateVersion: play.api.libs.json.Writes[RateVersion] = {
      new play.api.libs.json.Writes[io.flow.currency.v0.models.RateVersion] {
        def writes(obj: io.flow.currency.v0.models.RateVersion) = {
          jsObjectRateVersion(obj)
        }
      }
    }
  }
}

