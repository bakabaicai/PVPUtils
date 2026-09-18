package com.pvp_utils.client.gui.clickgui.widget;

import java.util.function.Consumer;
import java.util.function.Supplier;

public final class SettingPasswordBox extends SettingTextBox {
    private final Supplier<String> maskedValueSupplier;
    private final Consumer<String> saveConsumer;
    private String buffer = "";
    private boolean editing;
    private boolean dirty;

    public SettingPasswordBox(Supplier<String> maskedValueSupplier, Consumer<String> saveConsumer, int maxLength) {
        super(() -> "", value -> {}, maxLength);
        this.maskedValueSupplier = maskedValueSupplier;
        this.saveConsumer = saveConsumer;
    }

    @Override
    protected String getValue() {
        return editing ? buffer : maskedValueSupplier.get();
    }

    @Override
    protected String getDisplayText() {
        String value = getValue();
        return value.isEmpty() ? value : "*".repeat(value.length());
    }

    @Override
    protected void setValue(String value) {
        buffer = trimToMaxLength(value == null ? "" : value);
        dirty = true;
    }

    @Override
    public void update(float dt) {
        super.update(dt);
        if (focused == this && !editing) {
            editing = true;
            buffer = "";
            dirty = false;
        } else if (focused != this && editing) {
            editing = false;
            if (dirty) {
                saveConsumer.accept(buffer);
            }
            buffer = "";
        }
    }
}
