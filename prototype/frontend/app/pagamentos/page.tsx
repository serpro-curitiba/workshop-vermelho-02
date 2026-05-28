import { PaymentSimulator } from "@/components/payment-simulator";

export default function PaymentsPage() {
  return (
    <main className="stack">
      <section className="notice">
        <h2 className="section-title">Simulacao operacional</h2>
        <p>
          Use esta pagina para testar o endpoint <strong>/api/v1/pagamentos:calcular</strong> com um payload editavel.
          O dashboard e o catalogo usam Server Components; aqui a interacao ficou em um Client Component para facilitar a chamada manual.
        </p>
      </section>
      <PaymentSimulator />
    </main>
  );
}