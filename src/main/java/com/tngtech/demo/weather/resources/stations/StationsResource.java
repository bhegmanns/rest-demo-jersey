package com.tngtech.demo.weather.resources.stations;

import com.mercateo.common.rest.schemagen.JerseyResource;
import com.mercateo.common.rest.schemagen.link.LinkFactory;
import com.mercateo.common.rest.schemagen.link.LinkMetaFactory;
import com.mercateo.common.rest.schemagen.link.relation.Rel;
import com.mercateo.common.rest.schemagen.types.PaginatedList;
import com.mercateo.common.rest.schemagen.types.PaginatedResponse;
import com.mercateo.common.rest.schemagen.types.PaginatedResponseBuilderCreator;
import com.tngtech.demo.weather.domain.Station;
import com.tngtech.demo.weather.lib.schemagen.HyperSchemaCreator;
import com.tngtech.demo.weather.repositories.StationRepository;
import com.tngtech.demo.weather.resources.Paths;
import com.tngtech.demo.weather.resources.Roles;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.security.RolesAllowed;
import javax.inject.Inject;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Link;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.Optional;

@Path(Paths.STATIONS)
public class StationsResource implements JerseyResource {
    private static final Logger log = LoggerFactory.getLogger(StationsResource.class);

    @Inject
    private StationRepository stationRepository;

    @Inject
    private LinkMetaFactory linkMetaFactory;

    @Inject
    private StationLinkCreator stationLinkCreator;

    @Inject
    private PaginatedResponseBuilderCreator responseBuilderCreator;

    @Inject
    private HyperSchemaCreator hyperSchemaCreator;


    /**
     * Return a list of known airports as a json formatted list
     *
     * @return HTTP Response code and a json formatted list of IATA codes
     */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public PaginatedResponse<Station> getStations(@QueryParam(Paths.OFFSET) @DefaultValue("0") Integer offset,
                                                  @QueryParam(Paths.LIMIT) @DefaultValue("100") Integer limit) {
        log.debug("getStations({}, {})", offset, limit);
        offset = offset == null ? 0 : offset;
        limit = limit == null ? 2000 : limit;

        LinkFactory<StationsResource> stationsLinkFactory = linkMetaFactory.createFactoryFor(StationsResource.class);

        return responseBuilderCreator.<Station, Station>builder()
                .withList(new PaginatedList<>(
                        stationRepository.getTotalCount().intValue(),
                        offset,
                        limit,
                        stationRepository.getStations(offset, limit).toJavaList()))
                .withPaginationLinkCreator((rel, targetOffset, targetLimit) ->
                        stationsLinkFactory.forCall(rel, r -> r.getStations(targetOffset, targetLimit)))
                .withContainerLinks(
                        create(stationsLinkFactory))
                .withElementMapper(station ->
                        hyperSchemaCreator.create(station, stationLinkCreator.createForName(station.name)))
                .build();
    }

    private Optional<Link> create(LinkFactory<StationsResource> stationsLinkFactory) {
        return stationsLinkFactory.forCall(Rel.CREATE, r -> r.addStation(null));
    }

    /**
     * Add a new station to the known stations list.
     *
     * @param station new station parameters
     * @return HTTP Response code for the add operation
     */
    @POST
    @RolesAllowed(Roles.ADMIN)
    public Response addStation(Station station) {
        if (stationRepository.getStationByName(station.name).isDefined()) {
            log.debug("addStation({}, {}, {}) already exists", station.name, station.latitude(), station.longitude());
            return Response.status(Response.Status.FORBIDDEN).build();
        }
        log.debug("addStation({}, {}, {})", station.name, station.latitude(), station.longitude());

        stationRepository.addStation(station);

        return Response.status(Response.Status.OK).build();
    }

    @Path("/{" + Paths.STATION_NAME + "}")
    public Class<StationResource> stationSubResource() {
        log.trace("stationSubResource()");
        return StationResource.class;
    }
}