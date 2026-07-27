export function readStorageItem(key: string) {
  try {
    return globalThis.localStorage?.getItem(key) ?? null;
  } catch {
    return null;
  }
}

export function writeStorageItem(key: string, value: string) {
  try {
    globalThis.localStorage?.setItem(key, value);
  } catch {
    // Some embedded browsers disable storage. Auth still works for the active React session.
  }
}

export function removeStorageItem(key: string) {
  try {
    globalThis.localStorage?.removeItem(key);
  } catch {
    // Ignore storage cleanup failures in restricted browser contexts.
  }
}
