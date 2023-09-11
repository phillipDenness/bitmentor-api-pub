package bitmentor.api.service

import bitmentor.api.model.student.StudentPreferenceInsert
import bitmentor.api.repository.StudentPreferenceRepository
import io.ktor.util.*

@KtorExperimentalAPI
fun StudentPreferenceInsert.create(userId: Int) {
    StudentPreferenceRepository.save(this, userId)
}