/// <reference types="vite/client" />

// Type our custom env var so `import.meta.env.VITE_API_URL` is known to TypeScript.
interface ImportMetaEnv {
  readonly VITE_API_URL?: string;
}

interface ImportMeta {
  readonly env: ImportMetaEnv;
}
