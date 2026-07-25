import "@testing-library/jest-dom/vitest"

import { afterEach, vi } from "vitest"
import { cleanup } from "@testing-library/react"

afterEach(() => {
  cleanup()
})

if (!globalThis.ResizeObserver) {
  globalThis.ResizeObserver = class {
    observe() {}

    unobserve() {}

    disconnect() {}
  }
}

if (!globalThis.IntersectionObserver) {
  Object.defineProperty(globalThis, "IntersectionObserver", {
    configurable: true,
    value: class {
      readonly root = null
      readonly rootMargin = ""
      readonly thresholds = []

      observe() {}

      unobserve() {}

      disconnect() {}

      takeRecords() {
        return []
      }
    },
    writable: true,
  })
}

globalThis.requestAnimationFrame = (callback) =>
  globalThis.setTimeout(() => callback(performance.now()), 0)
globalThis.cancelAnimationFrame = (handle) => globalThis.clearTimeout(handle)

vi.stubGlobal(
  "matchMedia",
  vi.fn().mockImplementation((query: string) => ({
    matches: false,
    media: query,
    onchange: null,
    addEventListener: vi.fn(),
    removeEventListener: vi.fn(),
    addListener: vi.fn(),
    removeListener: vi.fn(),
    dispatchEvent: vi.fn(),
  })),
)
