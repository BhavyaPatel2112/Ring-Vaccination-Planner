import { useEffect, useState } from 'react'
import { apiGet } from './api/client.js'

function App() {
  const [backend, setBackend] = useState({ state: 'checking' })

  useEffect(() => {
    apiGet('/health')
      .then((data) => setBackend({ state: 'up', version: data.version }))
      .catch((err) => setBackend({ state: 'down', message: err.message }))
  }, [])

  return (
    <main className="page">
      <h1>Ring Vaccination Planner</h1>
      <p className="lead">
        Plan who to vaccinate around confirmed cases so every risky contact is protected,
        using as few doses as possible.
      </p>
      <p className={`status status-${backend.state}`}>
        {backend.state === 'checking' && 'Connecting to the backend…'}
        {backend.state === 'up' && `Backend connected (version ${backend.version})`}
        {backend.state === 'down' &&
          'Backend not reachable. Start it with ./mvnw spring-boot:run in the backend folder.'}
      </p>
    </main>
  )
}

export default App
