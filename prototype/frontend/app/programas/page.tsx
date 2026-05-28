import { getPrograms } from "@/lib/api";

export default async function ProgramsPage() {
  const programs = await getPrograms();

  return (
    <main className="table-card">
      <h2 className="section-title">Catalogo de Programas</h2>
      <p>Lista server-rendered usando App Router. Os dados sao buscados a cada request direto na API do backend.</p>
      <table>
        <thead>
          <tr>
            <th>Codigo</th>
            <th>Nome</th>
            <th>Tipo</th>
            <th>Valor base</th>
            <th>Reajuste</th>
            <th>Biometria</th>
          </tr>
        </thead>
        <tbody>
          {programs.length === 0 ? (
            <tr>
              <td colSpan={6}>Sem programas cadastrados ou backend indisponivel.</td>
            </tr>
          ) : (
            programs.map((program) => (
              <tr key={program.codigo}>
                <td>{program.codigo}</td>
                <td>{program.nome}</td>
                <td>{program.tipo}</td>
                <td>R$ {program.valorBaseAjustado.toFixed(2)}</td>
                <td>{(program.fatorReajuste * 100).toFixed(2)}%</td>
                <td>{program.exigeBiometria ? "Sim" : "Nao"}</td>
              </tr>
            ))
          )}
        </tbody>
      </table>
    </main>
  );
}