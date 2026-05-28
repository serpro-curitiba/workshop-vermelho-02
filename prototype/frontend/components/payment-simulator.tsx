"use client";

import { FormEvent, useState, useTransition } from "react";

import { apiBaseUrl } from "@/lib/api";

const samplePayload = {
  cpf: "98765432100",
  competencia: "202612",
  descontos: [
    { tipo: "JD", valor: 50.0 },
    { tipo: "IR", valor: 80.0, dtInicio: "2020-01-01", dtFim: "2099-01-01" }
  ]
};

export function PaymentSimulator() {
  const [payload, setPayload] = useState(JSON.stringify(samplePayload, null, 2));
  const [result, setResult] = useState<string>("Nenhuma simulacao executada.");
  const [isPending, startTransition] = useTransition();

  function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();

    startTransition(async () => {
      try {
        const response = await fetch(`${apiBaseUrl}/api/v1/pagamentos:calcular`, {
          method: "POST",
          headers: { "Content-Type": "application/json" },
          body: payload,
        });

        const data = await response.json();
        setResult(JSON.stringify(data, null, 2));
      } catch (error) {
        setResult(JSON.stringify({ message: "Falha ao chamar o backend", error }, null, 2));
      }
    });
  }

  return (
    <div className="form-card stack">
      <div>
        <h2 className="section-title">Simulador de Pagamento</h2>
        <p>O painel principal usa Server Components. Aqui fica a interacao direta para testar o endpoint de calculo com o payload do backend.</p>
      </div>
      <form onSubmit={handleSubmit} className="stack">
        <label>
          Payload JSON
          <textarea value={payload} onChange={(event) => setPayload(event.target.value)} />
        </label>
        <button type="submit" disabled={isPending}>{isPending ? "Calculando..." : "Calcular pagamento"}</button>
      </form>
      <pre className="result">{result}</pre>
    </div>
  );
}