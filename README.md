Project scaffold & mono-repo setup
Bootstrap the entire project structure — mono-repo layout, parent Maven POM, Docker Compose for local dev, and GitHub repository. Everything subsequent phases build on this.
Shared modules & data layer
Build the foundation all services depend on: JPA entities, Flyway migrations, Redis config, Kafka config, and all shared DTOs. Solid here means no rework later.
User & auth service
Implement JWT-based authentication with role-based access control. This unlocks all other services since they all depend on the JWT filter to identify callers.
Catalogue services (Movie, Theatre, Show)
Build the read-heavy catalogue services. The Show service is the most complex here — it owns SeatInventory and must integrate with Redis for seat count caching.
