package io.ruin.model.item;

/** One-for-one replacement of legacy coin items at all item entry points. */
public final class CoinCurrency {
    public static final int BLOOD_MONEY = 13307;

    private CoinCurrency() {}

    public static int normalize(int id) {
        // 996 is the legacy noted coins definition; 6964 is the alternate coin item.
        return id == 995 || id == 996 || id == 6964 ? BLOOD_MONEY : id;
    }
}
