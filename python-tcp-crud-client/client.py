import sys
import os
import socket
from google.protobuf.internal.encoder import _VarintBytes
from google.protobuf.internal.decoder import _DecodeVarint32

# Adiciona a pasta protoc para encontrar catalog_pb2 e catalog_pb2_grpc
sys.path.append(os.path.join(os.path.dirname(__file__), 'protoc'))

import catalog_pb2

def enviar_pacote(sock, mensagem_protobuf):
    dados_binarios = mensagem_protobuf.SerializeToString()
    sock.sendall(_VarintBytes(len(dados_binarios)) + dados_binarios)

def receber_pacote(sock, tipo_mensagem):
    bytes_tamanho = b''
    while True:
        b = sock.recv(1)
        if not b:
            return None
        bytes_tamanho += b
        if (b[0] & 0x80) == 0:
            break
    tamanho_pacote, _ = _DecodeVarint32(bytes_tamanho, 0)
    dados = b''
    while len(dados) < tamanho_pacote:
        pedaco = sock.recv(tamanho_pacote - len(dados))
        if not pedaco:
            return None
        dados += pedaco
    mensagem = tipo_mensagem()
    mensagem.ParseFromString(dados)
    return mensagem


def run():
    print("Iniciando cliente TCP...")
    
    # Endereço do servidor Kotlin
    host = 'localhost' 
    porta = 5090

    try:
        with socket.socket(socket.AF_INET, socket.SOCK_STREAM) as cliente_tcp:
            cliente_tcp.connect((host, porta))
            print(f"Link TCP estabelecido com o servidor em {host}:{porta}.\n")

            while True:
                print("\n" + "="*50)
                print("   PAINEL DE CONTROLE DE CATÁLOGO DE FILMES   ")
                print("="*50)
                print("1. Listar filmes por Ator")
                print("2. Listar filmes por Categoria")
                print("3. Buscar filme específico ......... (Get - por ID)")
                print("4. Inserir novo filme .............. (Create)")
                print("5. Atualizar filme existente ....... (Update)")
                print("6. Excluir filme ................... (Delete)")
                print("0. Desconectar")
                print("="*50)

                opcao = input("Escolha uma opção: ")

                request = catalog_pb2.TcpRequest()

                if opcao == "1":
                    ator = input("Informe o nome do ator (ex: Winsor McCay): ")
                    request.list_actor.actor_name = ator
                    
                    enviar_pacote(cliente_tcp, request)
                    response = receber_pacote(cliente_tcp, catalog_pb2.TcpResponse)

                    if not response.list.movies:
                        print("Nenhum registro localizado para este ator.")
                    for movie in response.list.movies:
                        print(f"[{movie.year}] {movie.title} - Elenco: {', '.join(movie.cast)}")

                elif opcao == "2":
                    categoria = input("Informe a categoria (ex: Animation, Comedy): ")
                    request.list_category.category = categoria
                    
                    enviar_pacote(cliente_tcp, request)
                    response = receber_pacote(cliente_tcp, catalog_pb2.TcpResponse)

                    if not response.list.movies:
                        print("Nenhum registro localizado para esta categoria.")
                    for movie in response.list.movies:
                        print(f"[{movie.year}] {movie.title} - Gêneros: {', '.join(movie.genres)}")

                elif opcao == "3":
                    filme_id = input("Informe o ID do filme (ObjectId): ")
                    request.get.id = filme_id
                    
                    enviar_pacote(cliente_tcp, request)
                    response = receber_pacote(cliente_tcp, catalog_pb2.TcpResponse)

                    if response.movie.id:
                        print("\n--- DADOS DO FILME ---")
                        print(f"Título: {response.movie.title}")
                        print(f"Ano: {response.movie.year}")
                        print(f"Resumo: {response.movie.plot}")
                        print(f"Poster: {response.movie.poster}")
                        print(f"Last updated: {response.movie.lastupdated}")
                        print(f"Elenco: {', '.join(response.movie.cast)}")                        
                        print(f"Gêneros: {', '.join(response.movie.genres)}")                        

                    else:
                        print("Filme não localizado nos registros do servidor.")

                elif opcao == "4":
                    print("\n--- INSERIR NOVO FILME ---")
                    titulo = input("Título da obra: ")
                    
                    try:
                        ano = int(input("Ano de lançamento: "))
                    except ValueError:
                        print("Erro: O ano deve ser um número inteiro.")
                        continue
                        
                    sinopse = input("Sinopse breve: ")
                    
                    # Monta o objeto Filme para envio
                    request.create.movie.title = titulo
                    request.create.movie.year = ano
                    request.create.movie.plot = sinopse
                    
                    enviar_pacote(cliente_tcp, request)
                    response = receber_pacote(cliente_tcp, catalog_pb2.TcpResponse)
                    print(f"\n[SUCESSO] Registro criado no MongoDB Atlas com o ID: {response.movie.id}")

                elif opcao == "5":
                    print("\n--- ATUALIZAR FILME EXISTENTE ---")
                    filme_id = input("ID do filme a ser atualizado (ObjectId): ")
                    novo_titulo = input("Novo título (deixe em branco para não alterar): ")
                    novo_ano_str = input("Novo ano (deixe em branco para não alterar): ")
                    
                    # Prepara o objeto com as atualizações
                    request.update.id = filme_id
                    if novo_titulo.strip():
                        request.update.movie.title = novo_titulo
                    if novo_ano_str.strip() and novo_ano_str.isdigit():
                        request.update.movie.year = int(novo_ano_str)
                        
                    enviar_pacote(cliente_tcp, request)
                    response = receber_pacote(cliente_tcp, catalog_pb2.TcpResponse)
                    
                    if response.movie.id:
                        print(f"\n[SUCESSO] Registro '{response.movie.title}' atualizado.")
                    else:
                        print("\n[FALHA] Não foi possível atualizar. Verifique se o ID existe.")

                elif opcao == "6":
                    print("\n--- EXCLUIR FILME ---")
                    filme_id = input("Aviso: Insira o ID do filme para exclusão: ")
                    
                    request.delete.id = filme_id
                    
                    enviar_pacote(cliente_tcp, request)
                    receber_pacote(cliente_tcp, catalog_pb2.TcpResponse)
                    
                    print(f"\n[SUCESSO] Filme com id = '{filme_id}' deletado com sucesso.")

                elif opcao == "0":
                    print("Encerrando conexão TCP")
                    break
                else:
                    print("Comando não reconhecido. Por favor, tente novamente.")

    except Exception as e:
        print(f"\n[ERRO] Falha na comunicação com o servidor TCP.\nDetalhes: {e}")
    except KeyboardInterrupt:
        print("\nSinal de interrupção manual recebido.")

if __name__ == "__main__":
    run()