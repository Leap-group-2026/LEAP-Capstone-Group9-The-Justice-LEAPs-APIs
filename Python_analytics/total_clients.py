import os
import pandas as pd
from sqlalchemy import create_engine
from dotenv import load_dotenv

load_dotenv()

db_user: str = os.getenv('DB_USER')
db_password: str = os.getenv('DB_PASSWORD')
db_host: str = os.getenv('DB_HOST')
db_port: str = os.getenv('DB_PORT')
db_name: str  = os.getenv('DB_NAME')

engine = create_engine(f'postgresql+psycopg2://{db_user}:{db_password}@{db_host}:{db_port}/{db_name}')

clients_df: pd.DataFrame = pd.read_sql('SELECT COUNT(*) FROM user_info;', engine)

num_clients: int = clients_df.at[0, 'count']

print("Total Clients:", num_clients)
