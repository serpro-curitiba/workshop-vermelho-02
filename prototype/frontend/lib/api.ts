export type Program = {
  codigo: string;
  nome: string;
  tipo: string;
  valorBaseOriginal: number;
  valorBaseAjustado: number;
  fatorReajuste: number;
  fatorK: number;
  exigeBiometria: boolean;
  fatoresRegionais: Array<{ uf: string; fatorRegional: number }>;
};

export type AuditEvent = {
  id: number;
  acao: string;
  entidade: string;
  entidadeId: string;
  descricao: string;
  criadoEm: string;
};

const apiBaseUrl = process.env.NEXT_PUBLIC_API_BASE_URL ?? "http://localhost:8080";

async function fetchJson<T>(path: string): Promise<T | null> {
  try {
    const response = await fetch(`${apiBaseUrl}${path}`, {
      cache: "no-store",
      headers: { Accept: "application/json" },
    });

    if (!response.ok) {
      return null;
    }

    return (await response.json()) as T;
  } catch {
    return null;
  }
}

export async function getPrograms(): Promise<Program[]> {
  return (await fetchJson<Program[]>("/api/v1/programas")) ?? [];
}

export async function getAudit(): Promise<AuditEvent[]> {
  return (await fetchJson<AuditEvent[]>("/api/v1/auditoria")) ?? [];
}

export { apiBaseUrl };