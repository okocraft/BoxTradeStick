package net.okocraft.boxtradestick;

import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.AbstractVillager;
import org.bukkit.persistence.PersistentDataAdapterContext;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.TestOnly;

public class TradeStickData {

    private static final NamespacedKey TRADE_STICK_DATA_KEY = Objects.requireNonNull(NamespacedKey.fromString("boxtradestick:trade_stick_data"));
    private static final PersistentDataType<PersistentDataContainer[], TradeStickData> TRADE_STICK_DATA_TYPE = new TradeStickDataType();

    public static @NotNull TradeStickData loadFrom(@NotNull AbstractVillager villager) {
        var data = villager.getPersistentDataContainer().get(TRADE_STICK_DATA_KEY, TRADE_STICK_DATA_TYPE);

        if (data != null && data.needUpdateVillager) {
            data.saveTo(villager);
        }

        return data != null ? data : new TradeStickData();
    }

    private static final NamespacedKey ID_KEY = Objects.requireNonNull(NamespacedKey.fromString("boxtradestick:user"));
    private static final NamespacedKey VALUE_KEY = Objects.requireNonNull(NamespacedKey.fromString("boxtradestick:value"));

    /* === Backward Compatibility === */
    private static final NamespacedKey OFFER_NUMBER_KEY = Objects.requireNonNull(NamespacedKey.fromString("boxtradestick:offer_number"));
    private static final NamespacedKey SCROLL_KEY = Objects.requireNonNull(NamespacedKey.fromString("boxtradestick:scroll"));

    private static final int SHIFTS_FOR_SCROLL = Integer.SIZE - 5;
    private static final int MASK_TO_CLEAR_SCROLL = (1 << SHIFTS_FOR_SCROLL) - 1;
    private static final int[] EMPTY_ARRAY = new int[0];

    public static final int MAXIMUM_INDEX = SHIFTS_FOR_SCROLL - 1;
    public static final int MAXIMUM_SCROLL = ~MASK_TO_CLEAR_SCROLL >>> SHIFTS_FOR_SCROLL;

    /*
        TradeStickData manages player's data (offer selection states and the scroll position) by int.

        The upper 5 bits represent the scroll position in the GUI.
        The other bits indicate whether the offer is selected.

        | scroll |    offer selection state    |
        |  00000 | 000000000000000000000000000 |
     */
    private final Object2IntMap<UUID> playerDataMap = new Object2IntOpenHashMap<>();

    /*
      This boolean value indicates whether the villager's data needs to be updated after data is loaded from the villager.

      This value will be true when data is loaded with legacy data that contains OFFER_NUMBER_KEY or SCROLL_KEY.
      When this value is true, the loadFrom method performs a save operation immediately after loading.
     */
    private boolean needUpdateVillager;

    public boolean isSelected(@NotNull UUID uuid, int index) {
        if (index < 0 || MAXIMUM_INDEX < index) {
            throw new IllegalArgumentException("index must be between 0 and " + MAXIMUM_INDEX + " (inclusive), but got " + index);
        }

        int state = playerDataMap.getInt(uuid);
        int bits = 1 << index;
        return (state & bits) == bits;
    }

    public int[] getSelectedIndices(@NotNull UUID uuid) {
        int state = playerDataMap.getInt(uuid);
        int size = Integer.bitCount(state & MASK_TO_CLEAR_SCROLL);

        if (size == 0) {
            return EMPTY_ARRAY;
        }

        int[] result = new int[size];
        int i = 0;

        for (int j = 0; j <= MAXIMUM_INDEX; j++) {
            int bits = 1 << j;
            if ((state & bits) == bits) {
                result[i++] = j;
            }
        }

        return result;
    }

    public void toggleOfferSelection(@NotNull UUID uuid, int index) {
        if (index < 0 || MAXIMUM_INDEX < index) {
            throw new IllegalArgumentException("index must be between 0 and " + MAXIMUM_INDEX + " (inclusive), but got " + index);
        }

        int state = playerDataMap.getInt(uuid);
        int bits = 1 << index;
        int newState = (state & bits) == bits ? state & ~bits : state | bits; // isSelected ? unselect : select
        playerDataMap.put(uuid, newState);
    }

    public int getScroll(@NotNull UUID uuid) {
        int state = playerDataMap.getInt(uuid);
        return state >>> SHIFTS_FOR_SCROLL;
    }

    public void setScroll(@NotNull UUID uuid, int scroll) {
        if (scroll < 0 || MAXIMUM_SCROLL < scroll) {
            throw new IllegalArgumentException("scroll must be between 0 and " + MAXIMUM_SCROLL + " (inclusive), but got " + scroll);
        }

        int state = playerDataMap.getInt(uuid);
        int newState = (state & MASK_TO_CLEAR_SCROLL) | (scroll << SHIFTS_FOR_SCROLL);
        playerDataMap.put(uuid, newState);
    }

    @TestOnly
    public int getBits(@NotNull UUID uuid) {
        return playerDataMap.getInt(uuid);
    }

    public void saveTo(@NotNull AbstractVillager villager) {
        villager.getPersistentDataContainer().set(TRADE_STICK_DATA_KEY, TRADE_STICK_DATA_TYPE, this);
    }

    private static class TradeStickDataType implements PersistentDataType<PersistentDataContainer[], TradeStickData> {

        @NotNull
        @Override
        public Class<PersistentDataContainer[]> getPrimitiveType() {
            return PersistentDataContainer[].class;
        }

        @NotNull
        @Override
        public Class<TradeStickData> getComplexType() {
            return TradeStickData.class;
        }

        @Override
        public PersistentDataContainer @NotNull [] toPrimitive(TradeStickData complex,
                                                               @NotNull PersistentDataAdapterContext context) {
            List<PersistentDataContainer> containerList = new ArrayList<>();

            for (Object2IntMap.Entry<UUID> entry : complex.playerDataMap.object2IntEntrySet()) {
                int value = entry.getIntValue();

                if (value == 0) {
                    continue;
                }

                PersistentDataContainer container = context.newPersistentDataContainer();

                container.set(ID_KEY, STRING, entry.getKey().toString());
                container.set(VALUE_KEY, INTEGER, value);

                containerList.add(container);
            }

            return containerList.toArray(PersistentDataContainer[]::new);
        }

        @Override
        public @NotNull TradeStickData fromPrimitive(PersistentDataContainer[] primitive,
                                                     @NotNull PersistentDataAdapterContext context) {
            TradeStickData data = new TradeStickData();

            for (PersistentDataContainer container : primitive) {
                UUID uuid = parseUuid(container.get(ID_KEY, STRING));

                if (uuid == null) {
                    continue;
                }

                if (container.has(OFFER_NUMBER_KEY) || container.has(SCROLL_KEY)) {
                    int offerNumber = container.getOrDefault(OFFER_NUMBER_KEY, INTEGER, -1);

                    if (0 <= offerNumber && offerNumber <= MAXIMUM_INDEX) {
                        data.toggleOfferSelection(uuid, offerNumber);
                    }

                    int scroll = container.getOrDefault(SCROLL_KEY, INTEGER, 0);

                    if (0 < scroll && scroll <= MAXIMUM_SCROLL) {
                        data.setScroll(uuid, scroll);
                    }

                    data.needUpdateVillager = true;
                } else {
                    int value = container.getOrDefault(VALUE_KEY, INTEGER, 0);
                    if (value != 0) {
                        data.playerDataMap.put(uuid, value);
                    }
                }
            }

            return data;
        }

        private @Nullable UUID parseUuid(@Nullable String value) {
            if (value != null) {
                try {
                    return UUID.fromString(value);
                } catch (IllegalArgumentException ignored) {
                }
            }
            return null;
        }
    }
}
