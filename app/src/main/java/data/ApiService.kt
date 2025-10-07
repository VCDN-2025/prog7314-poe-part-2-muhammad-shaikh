package za.co.studysync.data

import retrofit2.Response
import retrofit2.http.*

interface ApiService {
    @POST("api/v1/auth/google")
    suspend fun authGoogle(@Body body: Map<String, String>): AuthResponse

    @GET("api/v1/tasks")
    suspend fun getTasks(): List<TaskDto>

    @POST("api/v1/tasks")
    suspend fun createTask(@Body body: CreateTask): TaskDto

    @PUT("api/v1/tasks/{id}")
    suspend fun updateTask(@Path("id") id: String, @Body body: UpdateTask): TaskDto

    @DELETE("api/v1/tasks/{id}")
    suspend fun deleteTask(@Path("id") id: String): Response<Unit>
}

data class AuthResponse(val access_token: String, val user: UserDto)
data class UserDto(val sub: String?, val email: String?, val name: String?)

// Add dueDateTime (ISO 8601 string, e.g. 2025-10-07T14:30:00Z)
data class TaskDto(
    val id: String,
    val title: String,
    val status: String,
    val dueDateTime: String?,       // <-- NEW
    val createdAt: String?,
    val updatedAt: String?
)

data class CreateTask(
    val title: String,
    val dueDateTime: String?        // <-- NEW (nullable)
)

data class UpdateTask(
    val title: String? = null,
    val status: String? = null,
    val dueDateTime: String? = null // <-- NEW (optional on update)
)
