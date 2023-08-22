package com.example.avatar_ai_app.ar

class Graph : HashMap<String, Properties>()

data class Properties(
    val name: String,
    val edges: MutableList<Edge>
)

data class Edge(
    val destination: String,
    val distance: Int
)