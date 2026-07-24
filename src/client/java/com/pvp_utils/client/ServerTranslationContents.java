package com.pvp_utils.client;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.game.ServerboundSignUpdatePacket;
import net.minecraft.world.level.block.entity.SignText;

import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public final class ServerTranslationContents {
    private static final ReferenceQueue<TranslatableContents> QUEUE = new ReferenceQueue<>();
    private static final Set<IdentityWeakReference> SERVER_CONTENTS = ConcurrentHashMap.newKeySet();
    private static final Map<SignKey, String[]> PENDING_SIGN_UPDATES = new ConcurrentHashMap<>();

    private ServerTranslationContents() {
    }

    public static void markComponent(Component component) {
        mark(component, new IdentityHashMap<>());
    }

    public static boolean isServerContents(TranslatableContents contents) {
        purgeCollectedContents();
        return SERVER_CONTENTS.contains(new IdentityWeakReference(contents));
    }

    public static void trackSignText(BlockPos pos, boolean frontText, SignText text) {
        String[] lines = new String[4];
        for (int line = 0; line < lines.length; line++) {
            Component component = text.getMessage(line, false);
            markComponent(component);
            lines[line] = component.getString();
        }
        PENDING_SIGN_UPDATES.put(new SignKey(pos, frontText), lines);
    }

    public static ServerboundSignUpdatePacket replaceSignUpdate(ServerboundSignUpdatePacket packet) {
        String[] lines = PENDING_SIGN_UPDATES.remove(new SignKey(packet.getPos(), packet.isFrontText()));
        if (lines == null) {
            return packet;
        }
        return new ServerboundSignUpdatePacket(packet.getPos(), packet.isFrontText(), lines[0], lines[1], lines[2], lines[3]);
    }

    private static void mark(Object value, IdentityHashMap<Object, Boolean> visited) {
        if (value == null || visited.put(value, Boolean.TRUE) != null) {
            return;
        }
        if (value instanceof Component component) {
            if (component.getContents() instanceof TranslatableContents contents) {
                purgeCollectedContents();
                SERVER_CONTENTS.add(new IdentityWeakReference(contents, QUEUE));
                for (Object argument : contents.getArgs()) {
                    mark(argument, visited);
                }
            }
            for (Component sibling : component.getSiblings()) {
                mark(sibling, visited);
            }
            return;
        }
    }

    private static void purgeCollectedContents() {
        IdentityWeakReference reference;
        while ((reference = (IdentityWeakReference) QUEUE.poll()) != null) {
            SERVER_CONTENTS.remove(reference);
        }
    }

    private static final class IdentityWeakReference extends WeakReference<TranslatableContents> {
        private final int hashCode;

        private IdentityWeakReference(TranslatableContents contents) {
            super(contents);
            hashCode = System.identityHashCode(contents);
        }

        private IdentityWeakReference(TranslatableContents contents, ReferenceQueue<TranslatableContents> queue) {
            super(contents, queue);
            hashCode = System.identityHashCode(contents);
        }

        @Override
        public boolean equals(Object object) {
            return object instanceof IdentityWeakReference reference && get() == reference.get();
        }

        @Override
        public int hashCode() {
            return hashCode;
        }
    }

    private record SignKey(BlockPos pos, boolean frontText) {
    }
}
