package com.elziojunior.simplifiedbankingservice.load;

import static io.gatling.javaapi.core.CoreDsl.constantUsersPerSec;
import static io.gatling.javaapi.core.CoreDsl.global;
import static io.gatling.javaapi.core.CoreDsl.scenario;
import static io.gatling.javaapi.http.HttpDsl.http;
import static io.gatling.javaapi.http.HttpDsl.status;

import java.time.Duration;
import java.math.BigDecimal;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import io.gatling.javaapi.core.ScenarioBuilder;
import io.gatling.javaapi.core.Simulation;
import io.gatling.javaapi.http.HttpProtocolBuilder;

/** Generates deliberate write-lock contention against one sufficiently funded source account. */
public class HotSourceTransferSimulation extends Simulation {

    {
        TransferLoadSupport.validateDedicatedTarget();
    }

    private final long source = TransferLoadSupport.createAccount("Gatling hot source",
            String.valueOf(TransferLoadSupport.RATE * TransferLoadSupport.DURATION_SECONDS + 100));
    private final List<Long> destinations = java.util.stream.IntStream.range(0, TransferLoadSupport.DESTINATION_COUNT)
            .mapToObj(index -> TransferLoadSupport.createAccount("Gatling hot destination " + index, "0"))
            .toList();
    private final AtomicInteger nextDestination = new AtomicInteger();
    private final BigDecimal initialTotalBalance = TransferLoadSupport.totalBalance();

    private final HttpProtocolBuilder protocol = http.baseUrl(TransferLoadSupport.BASE_URL)
            .contentTypeHeader("application/json");

    private final ScenarioBuilder transfers = scenario("hot-source-transfers")
            .exec(session -> session
                    .set("token", TransferLoadSupport.issueToken().toString())
                    .set("source", source)
                    .set("destination", destinations.get(
                            Math.floorMod(nextDestination.getAndIncrement(), destinations.size()))))
            .exec(http("transfer from hot source")
                    .post("/api/v1/transfers")
                    .header("Idempotency-Key", "#{token}")
                    .body(io.gatling.javaapi.core.CoreDsl.StringBody(
                            "{\"sourceAccountId\":#{source},\"destinationAccountId\":#{destination},\"amount\":1.00}"))
                    .check(status().is(200)));

    {
        setUp(transfers.injectOpen(constantUsersPerSec(TransferLoadSupport.RATE)
                        .during(Duration.ofSeconds(TransferLoadSupport.DURATION_SECONDS))))
                .protocols(protocol)
                .assertions(global().failedRequests().count().is(0L));
    }

    @Override
    public void after() {
        TransferLoadSupport.assertMoneyConserved(initialTotalBalance);
    }

}
