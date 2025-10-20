package com.example.travel_planning.utils

import Place
import com.example.travel_planning.db.entities.PlaceEntity

 fun Place.toEntity(): PlaceEntity {
    return PlaceEntity(
        place_id = this.id,
        name = this.name,
        category = this.category,
        address = this.address,
        work_time = this.work_time.ifEmpty { null },
        description = this.description.ifEmpty { null },
        imageFilename = this.image_filename.ifEmpty { null }
    )
}