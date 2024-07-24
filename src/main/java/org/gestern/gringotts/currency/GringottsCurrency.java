package org.gestern.gringotts.currency;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.bukkit.Material;
import org.bukkit.Tag;
import org.bukkit.block.ShulkerBox;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BlockStateMeta;
import org.gestern.gringotts.Configuration;

/**
 * Representation of a currency. This contains information about the currency's denominations and their values.
 * The value is represented internally as "cents", that is, the smallest currency unit, and only gets transformed
 * into display value
 * for communication with the user or vault.
 *
 * @author jast
 */
public class GringottsCurrency {

    /**
     * Name of the currency.
     */
    private final String name;
    /**
     * Name of the currency, plural version.
     */
    private final String namePlural;
    /**
     * Currency unit divisor. Internally, all calculation is done in "cents".
     * This multiplier changes the external representation.
     * For instance, with unit 100, every cent will be worth 0.01 currency units
     */
    private final int unit;
    /**
     * Fractional digits supported by this currency.
     * For example, with 2 digits the minimum currency value would be 0.01
     */
    private final int digits;
    /**
     * Show balances and other currency values with individual denomination names.
     */
    private final boolean namedDenominations;
    private final Map<DenominationKey, Denomination> denoms = new HashMap<>();
    private final List<Denomination> sortedDenoms = new ArrayList<>();

    /**
     * Create currency.
     *
     * @param name               name of currency
     * @param namePlural         plural of currency name
     * @param digits             decimal digits used in currency
     * @param namedDenominations the named denominations
     */
    public GringottsCurrency(String name, String namePlural, int digits, boolean namedDenominations) {
        this.name = name;
        this.namePlural = namePlural;
        this.digits = digits;
        this.namedDenominations = namedDenominations;

        // calculate the "unit" from digits. It's just a power of 10!
        int d = digits, u = 1;
        while (d-- > 0) u *= 10;
        this.unit = u;
    }

    /**
     * Add a denomination and value to this currency.
     *
     * @param type           the denomination's item type
     * @param value          the denomination's value
     * @param unitName       the unit name
     * @param unitNamePlural the unit name plural
     */
    public void addDenomination(ItemStack type, double value, String unitName, String unitNamePlural) {
        DenominationKey k = new DenominationKey(type);
        Denomination d = new Denomination(k, getCentValue(value), unitName, unitNamePlural);
        denoms.put(k, d);
        // infrequent insertion, so I don't mind sorting on every insert
        sortedDenoms.add(d);
        Collections.sort(sortedDenoms);
    }


    /**
     * Get the value of an item stack in cents.
     * This is calculated by value_type * stacksize.
     * If the given item stack is not a valid denomination, the value is 0;
     *
     * @param stack a stack of items
     * @return the value of given stack of items
     */
    public long getValue(ItemStack stack) {
        if (stack == null || stack.getType() == Material.AIR) {
            return 0;
        }

        if (Configuration.CONF.includeShulkerBoxes && Tag.SHULKER_BOXES.isTagged(stack.getType())) {
            if (stack.getItemMeta() instanceof BlockStateMeta) {
                BlockStateMeta blockState = (BlockStateMeta) stack.getItemMeta();
                if (blockState.getBlockState() instanceof ShulkerBox) {
                    ShulkerBox shulker       = (ShulkerBox) blockState.getBlockState();
                    long       returnedValue = 0;

                    for (ItemStack content : shulker.getInventory().getContents()) {
                        returnedValue += getValue(content);
                    }

                    return returnedValue;
                }
            }
        }

        Denomination d = getDenominationOf(stack);
        return d != null ? d.getValue() * stack.getAmount() : 0;
    }

    /**
     * The display value for a given cent value.
     *
     * @param value value to calculate display value for
     * @return user representation of value
     */
    public double getDisplayValue(long value) {
        return (double) value / unit;
    }

    /**
     * The internal calculation value of a display value.
     *
     * @param value display value
     * @return Gringotts -internal value of given amount
     */
    public long getCentValue(double value) {
        return Math.round(value * unit);
    }


    /**
     * List of denominations used in this currency, in order of descending value.
     *
     * @return Unmodifiable List of denominations used in this currency, in order of descending value
     */
    public List<Denomination> getDenominations() {
        return Collections.unmodifiableList(sortedDenoms);
    }

    /**
     * Format string.
     *
     * @param formatString the format string
     * @param value        the value
     * @return the string
     */
    public String format(String formatString, double value) {

        if (namedDenominations) {

            StringBuilder b = new StringBuilder();

            long cv = getCentValue(value);

            for (Denomination denom : sortedDenoms) {
                long dv = cv / denom.getValue();
                cv %= denom.getValue();

                if (dv > 0) {
                    String display = dv + " " + (dv == 1L ? denom.getUnitName() : denom.getUnitNamePlural());
                    b.append(display);

                    if (cv > 0) {
                        b.append(", ");
                    }
                }
            }

            // might need this check for fractional values
            if (cv > 0 || b.length() == 0) {
                double displayVal = getDisplayValue(cv);

                b.append(String.format(formatString, displayVal, displayVal == 1.0 ? name : namePlural));
            }

            return b.toString();

        } else return String.format(formatString, value, value == 1.0 ? name : namePlural);

    }

    /**
     * Get the denomination of an item stack.
     *
     * @param stack the stack to get the denomination for
     * @return denomination for the item stack, or null if there is no such denomination
     */
    private Denomination getDenominationOf(ItemStack stack) {
        DenominationKey d = new DenominationKey(stack);
        if(Configuration.CONF.custommodeldataOnly) {
            for(Denomination dthis : getDenominations()){
                if(dthis.getKey().equals(d,true)) {
                    return dthis;
                }
            }
        }

        return denoms.get(d);
    }

    /**
     * To string string.
     *
     * @return the string
     */
    @Override
    public String toString() {
        return String.join("\n", sortedDenoms.stream().map(Denomination::toString).collect(Collectors.toSet()));
    }

    /**
     * Fractional digits supported by this currency.
     * For example, with 2 digits the minimum currency value would be 0.01
     *
     * @return the digits
     */
    public int getDigits() {
        return digits;
    }

    /**
     * Currency unit divisor. Internally, all calculation is done in "cents".
     * This multiplier changes the external representation.
     * For instance, with unit 100, every cent will be worth 0.01 currency units
     *
     * @return the unit
     */
    public int getUnit() {
        return unit;
    }

    /**
     * Name of the currency.
     *
     * @return the name
     */
    public String getName() {
        return name;
    }

    /**
     * Name of the currency, plural version.
     *
     * @return the name plural
     */
    public String getNamePlural() {
        return namePlural;
    }
}
