package net.okocraft.boxtradestick;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.UUID;

class TradeStickDataTest {

    private final TradeStickData tradeStickData = new TradeStickData();

    @Test
    void testToggleOfferSelection() {
        UUID uuid = UUID.randomUUID();

        Assertions.assertFalse(tradeStickData.isSelected(uuid, 0));
        Assertions.assertFalse(tradeStickData.isSelected(uuid, 1));

        tradeStickData.toggleOfferSelection(uuid, 1);

        Assertions.assertFalse(tradeStickData.isSelected(uuid, 0));
        Assertions.assertTrue(tradeStickData.isSelected(uuid, 1));
        Assertions.assertArrayEquals(new int[]{1}, tradeStickData.getSelectedIndices(uuid));
    }

    @Test
    void testScroll() {
        UUID uuid = UUID.randomUUID();

        Assertions.assertEquals(0, tradeStickData.getScroll(uuid));
        tradeStickData.setScroll(uuid, 5);
        Assertions.assertEquals(5, tradeStickData.getScroll(uuid));

        tradeStickData.setScroll(uuid, TradeStickData.MAXIMUM_SCROLL);
        Assertions.assertEquals(TradeStickData.MAXIMUM_SCROLL, tradeStickData.getScroll(uuid));
    }

    @Test
    void testBitState() {
        UUID uuid = UUID.randomUUID();

        tradeStickData.toggleOfferSelection(uuid, 1);
        tradeStickData.toggleOfferSelection(uuid, 5);
        tradeStickData.toggleOfferSelection(uuid, 10);
        Assertions.assertEquals(0b00000000000000000000010000100010, tradeStickData.getBits(uuid));

        tradeStickData.toggleOfferSelection(uuid, 26);
        Assertions.assertEquals(0b00000100000000000000010000100010, tradeStickData.getBits(uuid));

        tradeStickData.setScroll(uuid, 5);
        Assertions.assertEquals(0b00101100000000000000010000100010, tradeStickData.getBits(uuid));

        tradeStickData.toggleOfferSelection(uuid, 15);
        Assertions.assertEquals(0b00101100000000001000010000100010, tradeStickData.getBits(uuid));

        tradeStickData.setScroll(uuid, TradeStickData.MAXIMUM_SCROLL);
        Assertions.assertEquals(0b11111100000000001000010000100010, tradeStickData.getBits(uuid));

        Assertions.assertArrayEquals(new int[]{1, 5, 10, 15, 26}, tradeStickData.getSelectedIndices(uuid));
    }
}
