package com.pvp_utils.client.render.world;

import net.minecraft.util.ARGB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayDeque;
import java.util.Iterator;
import java.util.Objects;

public final class WorldParticles implements AutoCloseable {
    private final ArrayDeque<Particle> particles;
    private final int capacity;
    private boolean closed;

    public WorldParticles(int capacity) {
        if (capacity < 1 || capacity > 4096) throw new IllegalArgumentException("Particle capacity must be 1–4096");
        this.capacity = capacity;
        particles = new ArrayDeque<>(capacity);
    }

    public boolean emit(Vec3 position, Vec3 velocity, Spec spec) {
        if (closed || !WorldRender.valid(position) || !WorldRender.valid(velocity) || spec == null) return false;
        if (particles.size() == capacity) particles.removeFirst();
        particles.addLast(new Particle(position, velocity, spec));
        return true;
    }

    public void update(float seconds) {
        if (closed || !Float.isFinite(seconds) || seconds <= 0f) return;
        Iterator<Particle> iterator = particles.iterator();
        while (iterator.hasNext()) {
            Particle particle = iterator.next();
            particle.previousAge = particle.age;
            particle.age += seconds;
            if (particle.age >= particle.spec.lifetime()) {
                iterator.remove();
                continue;
            }
            particle.previousPosition = particle.position;
            particle.previousRotation = particle.rotation;
            double gravity = particle.spec.gravity();
            particle.position = particle.position.add(particle.velocity.scale(seconds))
                    .add(0, -0.5 * gravity * seconds * seconds, 0);
            particle.velocity = particle.velocity.add(0, -gravity * seconds, 0)
                    .scale(Math.exp(-particle.spec.drag() * seconds));
            particle.rotation += particle.spec.angularVelocity() * seconds;
        }
    }

    public void render(WorldRender.Frame frame) {
        render(frame, 128);
    }

    public void render(WorldRender.Frame frame, double maxDistance) {
        if (closed || frame == null || !Double.isFinite(maxDistance) || maxDistance <= 0) return;
        float partial = Math.clamp(frame.partialTick(), 0f, 1f);
        double maximumDistanceSquared = maxDistance * maxDistance;
        for (Particle particle : particles) {
            Vec3 position = particle.previousPosition.lerp(particle.position, partial);
            if (position.distanceToSqr(frame.cameraPosition()) > maximumDistanceSquared) continue;
            float age = particle.previousAge + (particle.age - particle.previousAge) * partial;
            float progress = Math.clamp(age / particle.spec.lifetime(), 0f, 1f);
            float alpha = (1f - progress) * (1f - progress);
            if (alpha < 0.005f) continue;
            float rotation = particle.previousRotation + (particle.rotation - particle.previousRotation) * partial;
            float size = particle.spec.size() * (1f - progress * 0.35f);
            int color = ARGB.multiplyAlpha(particle.spec.color(), alpha);
            WorldRender.billboard(position, size, size, rotation, color, particle.spec.shape(), particle.spec.throughWalls());
        }
    }

    public int size() {
        return particles.size();
    }

    public int capacity() {
        return capacity;
    }

    public void clear() {
        particles.clear();
    }

    @Override
    public void close() {
        clear();
        closed = true;
    }

    public record Spec(float lifetime, float size, int color, WorldRender.ParticleShape shape,
                       float gravity, float drag, float angularVelocity, boolean throughWalls) {
        public Spec {
            if (!Float.isFinite(lifetime) || lifetime <= 0f || !Float.isFinite(size) || size <= 0f
                    || !Float.isFinite(gravity) || !Float.isFinite(drag) || drag < 0f || !Float.isFinite(angularVelocity)) {
                throw new IllegalArgumentException("Invalid particle parameters");
            }
            Objects.requireNonNull(shape);
        }
    }

    private static final class Particle {
        private Vec3 previousPosition;
        private Vec3 position;
        private Vec3 velocity;
        private float previousRotation;
        private float rotation;
        private float previousAge;
        private float age;
        private final Spec spec;

        private Particle(Vec3 position, Vec3 velocity, Spec spec) {
            previousPosition = position;
            this.position = position;
            this.velocity = velocity;
            this.spec = spec;
        }
    }
}
