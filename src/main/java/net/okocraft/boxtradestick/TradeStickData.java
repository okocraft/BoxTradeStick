package net.okocraft.boxtradestick;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import org.bukkit.NamespacedKey;
import org.bukkit.persistence.PersistentDataAdapterContext;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.NotNull;

public class TradeStickData {
    public static final NamespacedKey TRADE_STICK_DATA_KEY =
            Objects.requireNonNull(NamespacedKey.fromString("boxtradestick:trade_stick_data"));

    private static final NamespacedKey ID_KEY = Objects.requireNonNull(NamespacedKey.fromString("boxtradestick:user"));
    private static final NamespacedKey OFFER_NUMBER_KEY = Objects.requireNonNull(NamespacedKey.fromString("boxtradestick:offer_number"));
    private static final NamespacedKey SCROLL_KEY = Objects.requireNonNull(NamespacedKey.fromString("boxtradestick:scroll"));

    private final Map<UUID, Integer> offerSelections = new HashMap<>();
    private final Map<UUID, Integer> scrolls = new HashMap<>();

    public TradeStickData() {
    }

    public TradeStickData(Map<UUID, Integer> offerSelections, Map<UUID, Integer> scrolls) {
        this.offerSelections.putAll(offerSelections);
        this.scrolls.putAll(scrolls);
    }

    public Map<UUID, Integer> getOfferSelections() {
        return this.offerSelections;
    }

    public Map<UUID, Integer> getScrolls() {
        return this.scrolls;
    }

    public static final PersistentDataType<PersistentDataContainer[], TradeStickData> TRADE_STICK_DATA_TYPE =
            new PersistentDataType<>() {

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
                    PersistentDataContainer container;

                    Set<UUID> ids = new HashSet<>();
                    ids.addAll(complex.offerSelections.keySet());
                    ids.addAll(complex.scrolls.keySet());

                    for (UUID id : ids) {
                        if (id == null) {
                            continue;
                        }
                        container = context.newPersistentDataContainer();
                        container.set(ID_KEY, STRING, id.toString());
                        container.set(OFFER_NUMBER_KEY, INTEGER, complex.offerSelections.getOrDefault(id, -1));
                        container.set(SCROLL_KEY, INTEGER, complex.scrolls.getOrDefault(id, 0));
                        containerList.add(container);
                    }

                    return containerList.toArray(PersistentDataContainer[]::new);
                }

                @Override
                public @NotNull TradeStickData fromPrimitive(PersistentDataContainer[] primitive,
                                                                                @NotNull PersistentDataAdapterContext context) {
                    Map<UUID, Integer> offerSelections = new HashMap<>();
                    Map<UUID, Integer> scrolls = new HashMap<>();
                    for (PersistentDataContainer container : primitive) {
                        try {
                            UUID id = UUID.fromString(container.getOrDefault(ID_KEY, STRING, "null"));
                            int offerNumber = container.getOrDefault(OFFER_NUMBER_KEY, INTEGER, -1);
                            int scroll = container.getOrDefault(SCROLL_KEY, INTEGER, 0);
                            if (offerNumber != -1) {
                                offerSelections.put(id, offerNumber);
                            }
                            if (scroll != 0) {
                                scrolls.put(id, scroll);
                            }
                        } catch (IllegalArgumentException ignored) {
                        }
                    }
                    return new TradeStickData(offerSelections, scrolls);
                }
            };
}