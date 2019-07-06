package com.github.davidmoten.etim;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.github.davidmoten.grumpy.core.Position;
import com.github.davidmoten.viem.EntityState;
import com.github.davidmoten.viem.System;

public final class Entities
        implements com.github.davidmoten.viem.System<IdentifierKey, String, Metadata> {

    private static final long MERGE_TIME_THRESHOLD_MS = TimeUnit.DAYS.toMillis(1);
    private static final long MIN_TIME_DIFF_FOR_SPEED_CHECK = TimeUnit.MINUTES.toMillis(1);
    private static final double MAX_DISTANCE_DIFF_KM = 30;

    private final Set<EntityState<IdentifierKey, String, Metadata>> entities = new HashSet<>();
    private final Map<KeyValue, EntityState<IdentifierKey, String, Metadata>> map = new HashMap<>();

    @Override
    public Iterable<EntityState<IdentifierKey, String, Metadata>> entityStates() {
        return entities;
    }

    @Override
    public Set<EntityState<IdentifierKey, String, Metadata>> matches(
            Map<IdentifierKey, String> identifiers) {
        return identifiers //
                .entrySet() //
                .stream() //
                .flatMap(entry -> {
                    KeyValue kv = new KeyValue(entry.getKey(), entry.getValue());
                    EntityState<IdentifierKey, String, Metadata> e = map.get(kv);
                    if (e == null) {
                        return Stream.empty();
                    } else {
                        return Stream.of(e);
                    }
                }).collect(Collectors.toSet());
    }

    @Override
    public boolean keyGreaterThan(IdentifierKey a, IdentifierKey b) {
        return a.priority() > b.priority();
    }

    @Override
    public boolean metadataGreaterThan(Metadata a, Metadata b) {
        return a.time() > b.time();
    }

    @Override
    public boolean mergeable(Metadata a, Metadata b) {
        if (a.type() != b.type()) {
            throw new IllegalArgumentException("cannot merge different entity types");
        }
        long timeDiffMs = Math.abs(a.time() - b.time());
        boolean expired = timeDiffMs > MERGE_TIME_THRESHOLD_MS;
        if (expired) {
            return true;
        }
        Position apos = Position.create(a.lat(), a.lon());
        Position bpos = Position.create(b.lat(), b.lon());
        double distanceKm = apos.getDistanceToKm(bpos);
        if (timeDiffMs < MIN_TIME_DIFF_FOR_SPEED_CHECK && distanceKm < MAX_DISTANCE_DIFF_KM) {
            return true;
        } else {
            double speedKmPerHour = distanceKm / timeDiffMs * 1000 * 60 * 60;
            return speedKmPerHour <= a.type().maxSpeedKmPerHour();
        }
    }

    @Override
    public Metadata merge(Metadata a, Metadata b) {
        Metadata max = a.time() > b.time() ? a : b;
        Metadata min = a.time() > b.time() ? b : a;
        Map<String, String> props = new HashMap<>(min.properties());
        for (Entry<String, String> entry : max.properties().entrySet()) {
            props.put(entry.getKey(), entry.getValue());
        }
        return new Metadata(max.type(), max.lat(), max.lon(), max.time(), props);
    }

    @Override
    public System<IdentifierKey, String, Metadata> update(
            List<EntityState<IdentifierKey, String, Metadata>> matches,
            Set<EntityState<IdentifierKey, String, Metadata>> newEntityStates) {
        entities.removeAll(matches);
        for (EntityState<IdentifierKey, String, Metadata> e : entities) {
            for (Entry<IdentifierKey, String> entry : e.identifiers().entrySet()) {
                map.remove(new KeyValue(entry.getKey(), entry.getValue()));
            }
        }
        entities.addAll(newEntityStates);
        for (EntityState<IdentifierKey, String, Metadata> e : newEntityStates) {
            for (Entry<IdentifierKey, String> entry : e.identifiers().entrySet()) {
                map.put(new KeyValue(entry.getKey(), entry.getValue()), e);
            }
        }
        return this;
    }

}
