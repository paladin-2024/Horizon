package com.horizon.linking;

import static com.horizon.linking.InstitutionType.BANK;
import static com.horizon.linking.InstitutionType.MOBILE_MONEY;

import java.util.List;

/**
 * The institutions Horizon ships with, for Uganda (UG) and the Democratic Republic of the Congo (CD).
 *
 * <p>{@code code} is Horizon's own stable slug, not a regulator's identifier: it never changes once released,
 * even if the institution is renamed, so linked accounts keep pointing at the right row. {@code name} is the
 * display name and may be corrected at any time — the seeder updates it in place.
 */
final class InstitutionSeedData {

    record Seed(String country, String code, String name, InstitutionType type) {
    }

    static final List<Seed> ALL = List.of(
            // --- Uganda: banks ---
            new Seed("UG", "stanbic-ug", "Stanbic Bank Uganda", BANK),
            new Seed("UG", "centenary-ug", "Centenary Bank", BANK),
            new Seed("UG", "absa-ug", "Absa Bank Uganda", BANK),
            new Seed("UG", "dfcu-ug", "dfcu Bank", BANK),
            new Seed("UG", "stanchart-ug", "Standard Chartered Bank Uganda", BANK),
            new Seed("UG", "equity-ug", "Equity Bank Uganda", BANK),
            new Seed("UG", "dtb-ug", "Diamond Trust Bank Uganda", BANK),
            new Seed("UG", "kcb-ug", "KCB Bank Uganda", BANK),
            new Seed("UG", "baroda-ug", "Bank of Baroda Uganda", BANK),
            new Seed("UG", "housing-finance-ug", "Housing Finance Bank", BANK),
            new Seed("UG", "postbank-ug", "PostBank Uganda", BANK),
            new Seed("UG", "citibank-ug", "Citibank Uganda", BANK),
            new Seed("UG", "ncba-ug", "NCBA Bank Uganda", BANK),
            new Seed("UG", "bank-of-africa-ug", "Bank of Africa Uganda", BANK),
            new Seed("UG", "ecobank-ug", "Ecobank Uganda", BANK),
            new Seed("UG", "uba-ug", "United Bank for Africa Uganda", BANK),
            new Seed("UG", "finance-trust-ug", "Finance Trust Bank", BANK),
            new Seed("UG", "tropical-ug", "Tropical Bank", BANK),
            new Seed("UG", "gtbank-ug", "Guaranty Trust Bank Uganda", BANK),
            new Seed("UG", "im-bank-ug", "I&M Bank Uganda", BANK),
            new Seed("UG", "exim-ug", "Exim Bank Uganda", BANK),
            new Seed("UG", "cairo-ug", "Cairo Bank Uganda", BANK),
            new Seed("UG", "abc-capital-ug", "ABC Capital Bank Uganda", BANK),
            new Seed("UG", "bank-of-india-ug", "Bank of India Uganda", BANK),
            new Seed("UG", "opportunity-ug", "Opportunity Bank Uganda", BANK),
            new Seed("UG", "salaam-ug", "Salaam Bank Uganda", BANK),

            // --- Uganda: mobile money ---
            new Seed("UG", "mtn-momo-ug", "MTN Mobile Money", MOBILE_MONEY),
            new Seed("UG", "airtel-money-ug", "Airtel Money", MOBILE_MONEY),

            // --- DR Congo: banks ---
            new Seed("CD", "rawbank-cd", "Rawbank", BANK),
            new Seed("CD", "tmb-cd", "Trust Merchant Bank", BANK),
            new Seed("CD", "equity-bcdc-cd", "EquityBCDC", BANK),
            new Seed("CD", "ecobank-cd", "Ecobank RDC", BANK),
            new Seed("CD", "bgfi-cd", "BGFIBank RDC", BANK),
            new Seed("CD", "sofibanque-cd", "Sofibanque", BANK),
            new Seed("CD", "afriland-cd", "Afriland First Bank CD", BANK),
            new Seed("CD", "access-cd", "Access Bank RDC", BANK),
            new Seed("CD", "uba-cd", "United Bank for Africa RDC", BANK),
            new Seed("CD", "fbn-cd", "FBNBank RDC", BANK),
            new Seed("CD", "advans-cd", "Advans Banque Congo", BANK),
            new Seed("CD", "standard-bank-cd", "Standard Bank RDC", BANK),

            // --- DR Congo: mobile money ---
            new Seed("CD", "mpesa-cd", "M-Pesa (Vodacom)", MOBILE_MONEY),
            new Seed("CD", "orange-money-cd", "Orange Money", MOBILE_MONEY),
            new Seed("CD", "airtel-money-cd", "Airtel Money", MOBILE_MONEY),
            new Seed("CD", "africell-money-cd", "Africell Money", MOBILE_MONEY));

    private InstitutionSeedData() {
    }
}
