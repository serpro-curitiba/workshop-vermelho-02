import { OverviewCard } from "@/components/overview-card";
import { getAudit, getPrograms } from "@/lib/api";

export default async function HomePage() {
  const [programs, audit] = await Promise.all([getPrograms(), getAudit()]);
  const biometricPrograms = programs.filter((program) => program.exigeBiometria).length;
  const averageAdjustedBase = programs.length === 0
    ? 0
    : programs.reduce((sum, item) => sum + item.valorBaseAjustado, 0) / programs.length;

  return (
    <main>
      <section className="hero">
        <article className="panel">
          <span className="kicker">Next.js 15 + Server Components</span>
          <h2 className="section-title">Painel operacional do SIFAP moderno</h2>
          <p>
            Esta interface consome o backend Spring Boot na renderizacao do servidor, concentrando programas sociais,
            rastros de auditoria e acesso rapido ao simulador de calculo.
          </p>
        </article>
        <aside className="notice warn">
          <h2 className="section-title">Pontos de execucao</h2>
          <p>Backend esperado em <strong>8080</strong>, frontend em <strong>3000</strong> no modo local ou <strong>3001</strong> no compose do root.</p>
          <p>Se a API ainda nao estiver no ar, os blocos abaixo continuam renderizando com listas vazias.</p>
        </aside>
      </section>

      <section className="metrics">
        <OverviewCard label="Programas" value={String(programs.length)} hint="Catalogo retornado por /api/v1/programas" />
        <OverviewCard label="Biometria" value={String(biometricPrograms)} hint="Programas que exigem validacao biometrica" />
        <OverviewCard label="Base media" value={`R$ ${averageAdjustedBase.toFixed(2)}`} hint="Media do valor base ajustado persistido" />
      </section>

      <section className="grid">
        <article className="table-card">
          <h2 className="section-title">Programas carregados</h2>
          <table>
            <thead>
              <tr>
                <th>Codigo</th>
                <th>Tipo</th>
                <th>Base ajustada</th>
                <th>Fator K</th>
              </tr>
            </thead>
            <tbody>
              {programs.length === 0 ? (
                <tr>
                  <td colSpan={4}>Nenhum programa disponivel no momento.</td>
                </tr>
              ) : (
                programs.slice(0, 6).map((program) => (
                  <tr key={program.codigo}>
                    <td>{program.codigo}</td>
                    <td><span className="pill">{program.tipo}</span></td>
                    <td>R$ {program.valorBaseAjustado.toFixed(2)}</td>
                    <td>{program.fatorK.toFixed(7)}</td>
                  </tr>
                ))
              )}
            </tbody>
          </table>
        </article>

        <article className="table-card">
          <h2 className="section-title">Ultimos eventos de auditoria</h2>
          <table>
            <thead>
              <tr>
                <th>Acao</th>
                <th>Entidade</th>
                <th>Descricao</th>
              </tr>
            </thead>
            <tbody>
              {audit.length === 0 ? (
                <tr>
                  <td colSpan={3}>Nenhum evento retornado pela API.</td>
                </tr>
              ) : (
                audit.slice(0, 6).map((event) => (
                  <tr key={event.id}>
                    <td><span className="pill">{event.acao}</span></td>
                    <td>{event.entidade}</td>
                    <td>{event.descricao}</td>
                  </tr>
                ))
              )}
            </tbody>
          </table>
        </article>
      </section>
    </main>
  );
}