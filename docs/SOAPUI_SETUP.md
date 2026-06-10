# Testing the SOAP API with SoapUI

> **About this document:** Step-by-step instructions for importing and calling the Oorsprong CountryInfo WSDL in SoapUI. Use this to verify SOAP behaviour before or alongside the REST application.
>
> **Update when:** The WSDL URL changes, SOAP operation names change, or the field mapping in `CountryInfoSoapClient` is modified.

The application calls two SOAP operations during a country import:

1. `CountryISOCode` — resolves a country name to an ISO code.
2. `FullCountryInfo` — returns detailed country data for that ISO code.

SoapUI lets you call these operations directly and inspect the raw XML, which is useful when debugging import failures or comparing SOAP output with what the REST API stores.

---

## Install SoapUI

1. Download SoapUI Open Source from [soapui.org/downloads](https://www.soapui.org/downloads/soapui/).
2. Run the installer for your platform.
3. Open SoapUI when installation finishes.

---

## Import the WSDL

1. Go to **File → New SOAP Project**.
2. Set the project name to `CountryInfoService`.
3. Paste this URL into **Initial WSDL**:

   ```
   http://webservices.oorsprong.org/websamples.countryinfo/CountryInfoService.wso?WSDL
   ```

4. Leave **Create Requests** enabled and click **OK**.

SoapUI will parse the WSDL and list every available operation under `CountryInfoServiceSoap`. You only need two of them for this project, but importing the full WSDL is the simplest approach.

---

## Call CountryISOCode

This is the first call the application makes. It corresponds to step 4 of the assessment brief.

1. In the project tree, expand **CountryInfoService → CountryInfoServiceSoap → CountryISOCode**.
2. Open **Request 1**.
3. Replace the request body with:

   ```xml
   <soap:Envelope xmlns:soap="http://schemas.xmlsoap.org/soap/envelope/">
     <soap:Body>
       <CountryISOCode xmlns="http://www.oorsprong.org/websamples.countryinfo">
         <sCountryName>Tanzania</sCountryName>
       </CountryISOCode>
     </soap:Body>
   </soap:Envelope>
   ```

4. Click the green **Play** button to send the request.

**Expected response:** a `CountryISOCodeResult` element containing `TZ`.

Try a few other names to see how the service behaves:

| Input | Expected ISO |
|-------|--------------|
| Kenya | KE |
| Germany | DE |
| United States | US |

If you send a name the service does not recognise, the response may contain a SOAP Fault rather than an empty result. Read the `faultstring` element for details.

---

## Call FullCountryInfo

This is the second call the application makes. It corresponds to step 5 of the assessment brief.

1. Expand **FullCountryInfo** and open **Request 1**.
2. Use the ISO code from the previous step:

   ```xml
   <soap:Envelope xmlns:soap="http://schemas.xmlsoap.org/soap/envelope/">
     <soap:Body>
       <FullCountryInfo xmlns="http://www.oorsprong.org/websamples.countryinfo">
         <sCountryISOCode>TZ</sCountryISOCode>
       </FullCountryInfo>
     </soap:Body>
   </soap:Envelope>
   ```

3. Send the request.

**Expected response:** a `FullCountryInfoResult` containing `tCountryInfo` with fields such as:

| SOAP field | Stored in application as |
|------------|--------------------------|
| `sISOCode` | `CountryInfo.isoCode` |
| `sName` | `CountryInfo.name` |
| `sCapitalCity` | `CountryInfo.capitalCity` |
| `sPhoneCode` | `CountryInfo.phoneCode` |
| `sContinentCode` | `CountryInfo.continentCode` |
| `sCurrencyISOCode` | `CountryInfo.currencyCode` |
| `sCountryFlag` | `CountryInfo.countryFlagUrl` |
| `Languages/tLanguage/sName` | `Language.name` (one row per language) |

The mapping logic lives in `src/main/java/com/frank/soap/CountryInfoSoapClient.java` and `src/main/java/com/frank/service/CountryInfoService.java`.

---

## Compare with the REST import endpoint

Once the SOAP calls look correct in SoapUI, start the Spring Boot application and send the same country through the REST layer:

```bash
curl -X POST http://localhost:8080/api/v1/countries/import \
  -H "Content-Type: application/json" \
  -d '{"name": "Tanzania"}'
```

The JSON response should contain the same core fields you saw in the `FullCountryInfo` SOAP response. If they differ, check application logs for parsing errors or SOAP faults.

---

## Common problems

**WSDL import fails**

The WSDL is hosted on the public internet. Confirm you have network access and that the URL has not changed. Try opening the WSDL URL in a browser — you should see XML.

**Empty or missing ISO code**

The country name may not match what the service expects. Try sentence case (`Kenya` rather than `KENYA`). Some country names need to match exactly (`United States`, not `USA`).

**SOAP Fault in the response**

Expand the fault details in SoapUI. Common causes: invalid country name, invalid ISO code, or a temporary service outage. Retry after a minute if the service itself appears down.

**Request times out**

Increase the socket timeout under **File → Preferences → HTTP Settings** in SoapUI, or retry later. The application uses 5 s connect and 15 s read timeouts by default (configurable in `application.yml`).

---

## Reference

| Item | Value |
|------|-------|
| WSDL | `http://webservices.oorsprong.org/websamples.countryinfo/CountryInfoService.wso?WSDL` |
| Namespace | `http://www.oorsprong.org/websamples.countryinfo` |
| Application SOAP client | `src/main/java/com/frank/soap/CountryInfoSoapClient.java` |
