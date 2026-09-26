package model;

import java.util.List;

/** Resultado generico de uma consulta: nomes das colunas + linhas. */
public record ResultadoConsulta(List<String> colunas, List<Object[]> linhas, long tempoMs) {}
