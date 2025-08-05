# Simulador de Tráfego Urbano com Semáforos Inteligentes

Este projeto implementa um simulador de tráfego urbano em Java, com foco na movimentação de carros (threads) através de um grid de ruas, semáforos (threads de controle) que ajustam seus tempos baseados no tráfego, detecção e resolução de congestionamentos, e priorização de veículos de emergência.

## Estrutura do Projeto

O código-fonte está organizado da seguinte forma:

```
/simulador_trafego
|-- /bin  (arquivos .class compilados)
|-- /src  (código-fonte .java)
|   |-- com/simuladortrafego/
|   |   |-- Car.java
|   |   |-- Direction.java
|   |   |-- EmergencyVehicle.java
|   |   |-- Grid.java
|   |   |-- Intersection.java
|   |   |-- LightState.java
|   |   |-- Simulator.java
|   |   |-- Street.java
|   |   |-- TrafficLight.java
|-- README.md (este arquivo)
|-- arquitetura_simulador.md (documento de arquitetura)
|-- roteiro_apresentacao.md (roteiro para vídeo de apresentação)
|-- todo.md (lista de tarefas do desenvolvimento)
```

## Requisitos

*   Java Development Kit (JDK) 17 ou superior.

## Compilação

1.  Navegue até o diretório raiz do projeto (`/home/ubuntu/simulador_trafego`).
2.  Compile os arquivos Java usando o seguinte comando:

    ```bash
    javac -d bin src/com/simuladortrafego/*.java
    ```
    Isso compilará todos os arquivos `.java` da pasta `src/com/simuladortrafego` e colocará os arquivos `.class` resultantes no diretório `bin`.

## Execução

1.  Após a compilação bem-sucedida, execute o simulador a partir do diretório raiz do projeto (`/home/ubuntu/simulador_trafego`) com o seguinte comando:

    ```bash
    java -cp bin com.simuladortrafego.Simulator
    ```
    O simulador iniciará e você verá os logs da simulação no console.

## Funcionalidades Implementadas

*   **Grid de Ruas Configurável:** O `Simulator.java` configura um grid com dois cruzamentos, incluindo uma via de mão única.
*   **Movimentação de Carros (Threads):** Carros são threads que se movem aleatoriamente pelo grid.
*   **Semáforos Inteligentes (Threads de Controle):**
    *   Cada cruzamento (`Intersection`) gerencia seus semáforos (`TrafficLight`).
    *   Os semáforos ajustam seus tempos com base na contagem de carros que passam no sinal amarelo, como um indicador de fluxo intenso.
*   **Detecção e Resolução de Congestionamentos (Básica):** A contagem de carros no amarelo é usada para ajustar os tempos dos semáforos, tentando aliviar o fluxo.
*   **Priorização de Veículos de Emergência:** `EmergencyVehicle` (subclasse de `Car`) pode solicitar prioridade nos cruzamentos, e os cruzamentos tentarão dar sinal verde para eles.
*   **Comunicação Distribuída (Arquitetura):** A arquitetura foi pensada para suportar RMI, embora a implementação atual seja local. A escolha entre RMI e Sockets foi deixada em aberto, com RMI sendo uma opção viável para a evolução do projeto.
*   **Visualização Textual:** A simulação exibe logs detalhados no console, mostrando o estado dos carros, semáforos e cruzamentos.

## Apresentação em Vídeo

O arquivo `roteiro_apresentacao.md` contém um guia detalhado para a criação de uma apresentação em vídeo de até 20 minutos, cobrindo a teoria utilizada, a arquitetura do sistema, detalhes da implementação e uma demonstração do programa funcionando.

## Próximos Passos (Sugestões)

*   Implementação completa da comunicação distribuída (RMI ou Sockets).
*   Desenvolvimento de uma interface gráfica (GUI) para melhor visualização.
*   Algoritmos de IA mais avançados para os semáforos.
*   Modelos de tráfego mais realistas.

