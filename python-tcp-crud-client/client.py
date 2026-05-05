import sys
import os

# Adiciona a pasta protoc para encontrar catalog_pb2 e catalog_pb2_grpc
sys.path.append(os.path.join(os.path.dirname(__file__), 'protoc'))

import catalog_pb2
import catalog_pb2_grpc
import grpc

def run():
    print("Sistemas online. Iniciando interface de comando Python...")
    
    # Endereço do servidor Kotlin
    server_address = 'localhost:5090' 

    try:
        with grpc.insecure_channel(server_address) as channel:
            stub = catalog_pb2_grpc.MovieServiceStub(channel)
            print(f"Link TCP estabelecido com o servidor central em {server_address}.\n")

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

                if opcao == "1":
                    ator = input("Informe o nome do ator (ex: Winsor McCay): ")
                    request = catalog_pb2.ListByActorRequest(actor_name=ator)
                    response = stub.ListMoviesByActor(request)

                    if not response.movies:
                        print("Nenhum registro localizado para este ator.")
                    for movie in response.movies:
                        print(f"[{movie.year}] {movie.title} - Elenco: {', '.join(movie.cast)}")

                elif opcao == "2":
                    categoria = input("Informe a categoria (ex: Animation, Comedy): ")
                    request = catalog_pb2.ListByCategoryRequest(category=categoria)
                    response = stub.ListMoviesByCategory(request)

                    if not response.movies:
                        print("Nenhum registro localizado para esta categoria.")
                    for movie in response.movies:
                        print(f"[{movie.year}] {movie.title} - Gêneros: {', '.join(movie.genres)}")

                elif opcao == "3":
                    filme_id = input("Informe o ID do filme (ObjectId): ")
                    request = catalog_pb2.GetMovieRequest(id=filme_id)
                    response = stub.GetMovie(request)

                    if response.id:
                        print("\n--- DADOS DO FILME ---")
                        print(f"Título: {response.title}")
                        print(f"Ano: {response.year}")
                        print(f"Resumo: {response.plot}")
                        print(f"Poster: {response.poster}")
                        print(f"Last updated: {response.lastupdated}")
                        print(f"Elenco: {', '.join(response.cast)}")                        
                        print(f"Gêneros: {', '.join(response.genres)}")                        

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
                    novo_filme = catalog_pb2.Movie(title=titulo, year=ano, plot=sinopse)
                    request = catalog_pb2.CreateMovieRequest(movie=novo_filme)
                    
                    response = stub.CreateMovie(request)
                    print(f"\n[SUCESSO] Registro criado no MongoDB Atlas com o ID: {response.id}")

                elif opcao == "5":
                    print("\n--- ATUALIZAR FILME EXISTENTE ---")
                    filme_id = input("ID do filme a ser atualizado (ObjectId): ")
                    novo_titulo = input("Novo título (deixe em branco para não alterar): ")
                    novo_ano_str = input("Novo ano (deixe em branco para não alterar): ")
                    
                    # Prepara o objeto com as atualizações
                    filme_atualizado = catalog_pb2.Movie()
                    if novo_titulo.strip():
                        filme_atualizado.title = novo_titulo
                    if novo_ano_str.strip() and novo_ano_str.isdigit():
                        filme_atualizado.year = int(novo_ano_str)
                        
                    request = catalog_pb2.UpdateMovieRequest(id=filme_id, movie=filme_atualizado)
                    
                    response = stub.UpdateMovie(request)
                    if response.id:
                        print(f"\n[SUCESSO] Registro '{response.title}' atualizado.")
                    else:
                        print("\n[FALHA] Não foi possível atualizar. Verifique se o ID existe.")

                elif opcao == "6":
                    print("\n--- EXCLUIR FILME ---")
                    filme_id = input("Aviso: Insira o ID do filme para exclusão: ")
                    
                    request = catalog_pb2.DeleteMovieRequest(id=filme_id)
                    
                    stub.DeleteMovie(request)
                    print(f"\n[SUCESSO] Filme com id = '{filme_id}' deletado com sucesso.")

                elif opcao == "0":
                    print("Encerrando conexão TCP")
                    break
                else:
                    print("Comando não reconhecido. Por favor, tente novamente.")

    except grpc.RpcError as e:
        print(f"\n[ERRO] Falha na comunicação com o servidor gRPC.\nDetalhes: {e.details()}")
    except KeyboardInterrupt:
        print("\nSinal de interrupção manual recebido.")

if __name__ == "__main__":
    run()