package org.vitbuk.draw

import org.vitbuk.model.DrawResult
import org.vitbuk.model.Participant
import java.security.SecureRandom
import java.time.Instant

class SattoloDrawAlgorithm(
    private val allowTwoParticipants: Boolean = true,
    private val random: SecureRandom = SecureRandom(),
    private val reseedEachDraw: Boolean = false,
    private val seedSupplier: () -> Long = {
        Instant.now().toEpochMilli() xor System.nanoTime()
    }
) : SecretSantaDrawAlgorithm {

    override fun draw(participants: List<Participant>): DrawResult {
        if (reseedEachDraw) {
            random.setSeed(seedSupplier())
        }

        val ids = participants.map { it.userId }
        require(ids.size >= 2) { "Нужно хотя бы 2 участни:цы" }

        if (ids.size == 2) {
            if (!allowTwoParticipants) {
                error("С 2 участни:цами только взаимный обмен возможен. Добавьте больше людей.")
            }
            val assignments = mapOf(ids[0] to ids[1], ids[1] to ids[0])
            return DrawResult(
                assignments = assignments,
                warnings = listOf("В ивенте 2 участни:цы — обмен неизбежно взаимный.")
            )
        }

        val cycle = sattoloCycle(ids)

        val assignments = LinkedHashMap<Long, Long>(cycle.size)
        for (i in cycle.indices) {
            val giver = cycle[i]
            val receiver = cycle[(i + 1) % cycle.size]
            assignments[giver] = receiver
        }

        sanityCheck(ids, assignments)
        return DrawResult(assignments = assignments)
    }

    private fun sattoloCycle(ids: List<Long>): List<Long> {
        val a = ids.toMutableList()
        for (i in a.size - 1 downTo 1) {
            val j = random.nextInt(i)
            val tmp = a[i]
            a[i] = a[j]
            a[j] = tmp
        }
        return a
    }

    private fun sanityCheck(originalIds: List<Long>, assignments: Map<Long, Long>) {
        check(assignments.keys.size == originalIds.size) { "Not all givers assigned" }
        check(assignments.values.toSet().size == originalIds.size) { "Receivers are not unique" }
        check(assignments.none { (g, r) -> g == r }) { "Self-gifting detected" }
        check(assignments.keys.containsAll(originalIds)) { "Some givers missing" }
        check(assignments.values.containsAll(originalIds)) { "Some receivers missing" }
    }
}