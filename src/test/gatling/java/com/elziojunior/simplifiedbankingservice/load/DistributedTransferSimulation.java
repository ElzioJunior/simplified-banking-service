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
import java.util.stream.IntStream;

import io.gatling.javaapi.core.ScenarioBuilder;
import io.gatling.javaapi.core.Simulation;
import io.gatling.javaapi.http.HttpProtocolBuilder;

/** Measures transfer throughput across independent account pairs without a deliberate hot row. */
public class DistributedTransferSimulation extends Simulation {

    {
        TransferLoadSupport.validateDedicatedTarget();
    }

    private final int pairCount = Math.max(TransferLoadSupport.DESTINATION_COUNT, TransferLoadSupport.RATE * 2);
    private final List<AccountPair> pairs = IntStream.range(0, pairCount)
            .mapToObj(index -> new AccountPair(
                    TransferLoadSupport.createAccount("Gatling distributed source " + index,
                            String.valueOf(TransferLoadSupport.DURATION_SECONDS + 100)),
                    TransferLoadSupport.createAccount("Gatling distributed destination " + index, "0")))
            .toList();
    private final AtomicInteger nextPair = new AtomicInteger();
    private final BigDecimal initialTotalBalance = TransferLoadSupport.totalBalance();

    private final HttpProtocolBuilder protocol = http.baseUrl(TransferLoadSupport.BASE_URL)
            .contentTypeHeader("application/json");

    private final ScenarioBuilder transfers = scenario("distributed-transfers")
            .exec(session -> {
                AccountPair pair = pairs.get(Math.floorMod(nextPair.getAndIncrement(), pairs.size()));
                return session
                        .set("token", TransferLoadSupport.issueToken().toString())
                        .set("source", pair.source())
                        .set("destination", pair.destination());
            })
            .exec(http("transfer across distributed pair")
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

    private record AccountPair(long source, long destination) {
    }
}
