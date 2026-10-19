package com.horizon.linking;

import com.horizon.common.error.ApiException;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.stereotype.Component;

/** Resolves the {@link BankProvider} for a {@link ProviderType}. Every provider bean registers itself. */
@Component
public class ProviderRegistry {

    private final Map<ProviderType, BankProvider> byType;

    ProviderRegistry(List<BankProvider> providers) {
        Map<ProviderType, BankProvider> map = new EnumMap<>(ProviderType.class);
        for (BankProvider provider : providers) {
            BankProvider previous = map.put(provider.type(), provider);
            if (previous != null) {
                throw new IllegalStateException("Two providers claim " + provider.type() + ": "
                        + previous.getClass().getName() + " and " + provider.getClass().getName());
            }
        }
        this.byType = Map.copyOf(map);
    }

    public Optional<BankProvider> find(ProviderType type) {
        return Optional.ofNullable(byType.get(type));
    }

    /** @throws ApiException 422 {@code provider_unsupported} when no provider is registered for the type */
    public BankProvider get(ProviderType type) {
        return find(type).orElseThrow(() -> ApiException.unprocessable(
                "provider_unsupported", "No provider is available for " + type.name()));
    }
}
