package uk.gov.justice.tools.eventsourcing.transformation.service;

import uk.gov.justice.services.eventsourcing.source.core.EventSource;
import uk.gov.justice.services.eventsourcing.source.core.EventSourceTransformation;
import uk.gov.justice.services.eventsourcing.source.core.EventStream;
import uk.gov.justice.services.eventsourcing.source.core.exception.EventStreamException;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.tools.eventsourcing.transformation.StreamTransformerUtil;
import uk.gov.justice.tools.eventsourcing.transformation.api.EventTransformation;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.slf4j.Logger;

@ApplicationScoped
public class StreamTransformer {

    @Inject
    private Logger logger;

    @Inject
    private EventSource eventSource;

    @Inject
    private EventSourceTransformation eventSourceTransformation;

    @Inject
    private StreamAppender streamAppender;

    @Inject
    private StreamTransformerUtil streamTransformerUtil;

    @SuppressWarnings({"squid:S2629"})
    public void transformStream(final UUID streamId, final Set<EventTransformation> transformations) throws EventStreamException {
        final EventStream stream = eventSource.getStreamById(streamId);

        try (final Stream<JsonEnvelope> events = stream.read()) {
            final List<JsonEnvelope> originalEvents = events.collect(Collectors.toList());

            eventSourceTransformation.clearStream(streamId);

            logger.debug("Transforming events on stream {}", streamId);

            try (Stream<JsonEnvelope> transformedEventStream = streamTransformerUtil.transform(originalEvents.stream(), transformations)) {
                streamAppender.appendEventsToStream(streamId, transformedEventStream);
            }
        }
    }
}
