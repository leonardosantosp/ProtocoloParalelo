# ProtocoloParalelo

Este projeto implementa uma comunicação entre dois computadores utilizando o protocolo de envio de pacotes UDP com controle de fluxo e confirmação (ACKs), simulando um cenário de transmissão de dados entre um servidor (envio de dados) e um cliente (recepção de dados).

O servidor envia dados de um arquivo de entrada para o cliente, utilizando um protocolo de janela deslizante com número de sequência para garantir a ordem e a confiabilidade da transmissão. O servidor envia pacotes contendo dados e o número de sequência, e o cliente responde com ACKs (acknowledgments), confirmando o recebimento correto de cada pacote.
