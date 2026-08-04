package com.ridecopilot.app.data

import com.ridecopilot.app.domain.RideOffer
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow

object RideOfferBus {
    private val _offers = MutableSharedFlow<RideOffer>(extraBufferCapacity = 1)
    val offers: SharedFlow<RideOffer> = _offers

    fun tryPublish(offer: RideOffer) {
        _offers.tryEmit(offer)
    }
}
