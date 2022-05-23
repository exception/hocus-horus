package com.joinhocus.horus.misc;

import com.joinhocus.horus.misc.function.QuadFunction;
import com.joinhocus.horus.misc.function.TriFunction;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Collectors;

public final class CompletableFutures {

    /**
     * Composes a list of typed CompletionStages into a single CompletableFuture with the
     * results of said futures.
     *
     * @param list The raw list of futures which we'll use to compose the new one.
     * @param <T>  the type of objects
     *
     * @return a {@link CompletableFuture} with a single list of all the values returned from the
     * completion of the list of futures.
     */
    public static <T> CompletableFuture<List<T>> asList(List<? extends CompletionStage<? extends T>> list) {
        // noinspection unchecked
        CompletableFuture<? extends T>[] asArray = new CompletableFuture[list.size()];
        for (int i = 0; i < list.size(); i++) {
            asArray[i] = list.get(i).toCompletableFuture();
        }
        return CompletableFuture.allOf(asArray).thenApply(future -> {
            List<T> results = new ArrayList<>(list.size());
            for (CompletableFuture<? extends T> f : asArray) {
                results.add(f.join());
            }
            return results;
        });
    }

    /**
     * Composes a map of typed CompletionStages into a single map of the
     * successful values of the input futures, wrapped into a CompletableFuture.
     */
    public static <Input, Output> CompletableFuture<Map<Input, Output>> asMap(
            Map<Input, ? extends CompletionStage<? extends Output>> map
    ) {
        List<Input> keys = new ArrayList<>(map.keySet());
        //noinspection unchecked
        CompletableFuture<? extends Output>[] values = new CompletableFuture[keys.size()];
        for (int i = 0; i < keys.size(); i++) {
            values[i] = map.get(keys.get(i)).toCompletableFuture();
        }

        return CompletableFuture.allOf(values).thenApply(aVoid -> {
            Map<Input, Output> out = new HashMap<>(values.length);
            for (int i = 0; i < values.length; i++) {
                out.put(keys.get(i), values[i].join());
            }
            return out;
        });
    }

    /**
     * Returns a new CompletableFuture with a composed list of the successful values
     * of the provided stages. If a stage fails the mapping function is called and the value
     * is returned and placed into the list.
     *
     * @return the composed future.
     */
    public static <T> CompletableFuture<List<T>> asSuccessfulList(
            List<? extends CompletionStage<T>> stages,
            Function<Throwable, ? extends T> function) {
        // collect the future values as a list and then run Futures#asList on it.
        return stages.stream().map(f -> f.exceptionally(function)).collect(
                Collectors.collectingAndThen(
                        Collectors.toList(),
                        CompletableFutures::asList
                )
        );
    }

    /**
     * Combines three stages of a future by applying a combining function
     *
     * @return the completed stage.
     */
    public static <First, Second, Third> CompletionStage<First> combine(
            CompletionStage<Second> second,
            CompletionStage<Third> third,
            BiFunction<Second, Third, First> function) {
        return second.thenCombine(third, function);
    }

    public static <First, Second, Third, Fourth> CompletionStage<First> combine(
            CompletionStage<Second> second,
            CompletionStage<Third> third,
            CompletionStage<Fourth> fourth,
            TriFunction<Second, Third, Fourth, First> function) {

        CompletableFuture<Second> secondAsFuture = second.toCompletableFuture();
        CompletableFuture<Third> thirdAsFuture = third.toCompletableFuture();
        CompletableFuture<Fourth> fourthAsFuture = fourth.toCompletableFuture();

        return CompletableFuture.allOf(secondAsFuture, thirdAsFuture, fourthAsFuture)
                .thenApply(ignored -> function.apply(secondAsFuture.join(), thirdAsFuture.join(), fourthAsFuture.join()));
    }

    public static <First, Second, Third, Fourth, Fifth> CompletionStage<First> combine(
            CompletionStage<Second> second,
            CompletionStage<Third> third,
            CompletionStage<Fourth> fourth,
            CompletableFuture<Fifth> fifth,
            QuadFunction<Second, Third, Fourth, Fifth, First> function) {

        CompletableFuture<Second> secondAsFuture = second.toCompletableFuture();
        CompletableFuture<Third> thirdAsFuture = third.toCompletableFuture();
        CompletableFuture<Fourth> fourthAsFuture = fourth.toCompletableFuture();
        CompletableFuture<Fifth> fifthAsFuture = fifth.toCompletableFuture();

        return CompletableFuture.allOf(secondAsFuture, thirdAsFuture, fourthAsFuture, fifthAsFuture)
                .thenApply(ignored -> function.apply(secondAsFuture.join(), thirdAsFuture.join(), fourthAsFuture.join(), fifthAsFuture.join()));
    }

    /**
     * Wraps a Throwable into a Future.
     *
     * @return a "failed" CompletableFuture.
     */
    public static <T> CompletableFuture<T> failedFuture(Throwable throwable) {
        final CompletableFuture<T> future = new CompletableFuture<>();
        future.completeExceptionally(throwable);
        return future;
    }

    /**
     * Creates a stage which (if completed exceptionally) will pass on the exception,
     * if we succeed the stage completes with the passed value.
     *
     * @return the new stage.
     */
    public static <T> CompletionStage<T> failedCompose(
            CompletionStage<T> complex,
            Function<Throwable, ? extends CompletionStage<T>> func) {
        CompletionStage<CompletionStage<T>> wrapped = complex.thenApply(CompletableFuture::completedFuture);
        return wrapped.exceptionally(func).thenCompose(Function.identity());
    }

    public static <T> CompletionStage<T> unwrap(
            CompletionStage<? extends CompletionStage<T>> stage
    ) {
        return stage.thenCompose(Function.identity());
    }

}
